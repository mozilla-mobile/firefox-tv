/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.NONE
import androidx.lifecycle.ProcessLifecycleOwner
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Deferred
import mozilla.appservices.fxaclient.Config
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.Avatar
import mozilla.components.concept.sync.DeviceCapability
import mozilla.components.concept.sync.DeviceEvent
import mozilla.components.concept.sync.DeviceEventsObserver
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.DeviceConfig
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AuthenticatedNoProfile
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AuthenticatedWithProfile
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NeedsReauthentication
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NotAuthenticated

private val logger = Logger("FxaRepo")

private val APPLICATION_SCOPES = setOf(
    // We don't use sync, however, if we later add the sync scope, already authenticated users
    // will have to log in again. To avoid this in the future, we preemptively declare a sync scope.
    "https://identity.mozilla.com/apps/oldsync"
)

/**
 * Manages Firefox Account (FxA) state. In particular, [accountState] exposes the current sign in
 * state of the user. If you want to initiate sign in, see [FxaLoginUseCase.beginLogin].
 *
 * Devs should use this class rather than interacting with the FxA library directly.
 */
class FxaRepo(
    context: Context,
    val accountManager: FxaAccountManager = newInstanceDefaultAccountManager(context)
) {

    /**
     * The account profile is fetched asynchronously.
     * There is no way to transition from [NotAuthenticated] directly to [AuthenticatedWithProfile];
     * [AuthenticatedNoProfile] is always a state in between. Even if the account is saved to disk,
     * the profile is not and needs to be fetched.
     */
    sealed class AccountState {
        // TODO: Later, may need "failed to login": https://github.com/mozilla-mobile/android-components/issues/3712
        /**
         *  After the profile is fetched async
         */
        data class AuthenticatedWithProfile(val profile: FxaProfile) : AccountState()
        /**
         *  Before the profile is fetched async.
         *  If the profile is null, this is the resulting state.
         */
        object AuthenticatedNoProfile : AccountState() // Before the profile is fetched async
        object NeedsReauthentication : AccountState()
        object NotAuthenticated : AccountState() // Initial state
    }

    @VisibleForTesting(otherwise = NONE)
    val accountObserver = FirefoxAccountObserver()

    private val _accountState: BehaviorSubject<AccountState> = BehaviorSubject.createDefault(NotAuthenticated)
    val accountState: Observable<AccountState> = _accountState.hide()

    init {
        accountManager.register(accountObserver)
        accountManager.registerForDeviceEvents(FirefoxDeviceEventsObserver(), ProcessLifecycleOwner.get(),
            autoPause = false /* Avoid pausing even when the app is backgrounded. */)

        @Suppress("DeferredResultUnused") // No value is returned & we don't need to wait for this to complete.
        accountManager.initAsync() // If user is already logged in, the appropriate observers will be triggered.
    }

    /**
     * Notifies the FxA library that login is starting: callers should generally call [FxaLoginUseCase.beginLogin]
     * instead of this method.
     */
    fun beginLoginInternalAsync(): Deferred<String?> {
        return accountManager.beginAuthenticationAsync()
    }

    /**
     * See [AccountState] kdoc for more explanation on states.
     */
    @VisibleForTesting(otherwise = NONE)
    inner class FirefoxAccountObserver : AccountObserver {
        override fun onAuthenticated(account: OAuthAccount, newAccount: Boolean) {
            _accountState.onNext(AuthenticatedNoProfile)
        }

        override fun onAuthenticationProblems() {
            _accountState.onNext(NeedsReauthentication)
        }

        override fun onLoggedOut() {
            _accountState.onNext(NotAuthenticated)
        }

        /**
         * This is called when the profile is first fetched after sign-in.
         */
        override fun onProfileUpdated(profile: Profile) {
            _accountState.onNext(AuthenticatedWithProfile(profile.toDomainObject()))
        }
    }

    private inner class FirefoxDeviceEventsObserver : DeviceEventsObserver {
        override fun onEvents(events: List<DeviceEvent>) {
            logger.debug("received device events: $events")
        }
    }

    companion object {
        private const val CLIENT_ID = "85da77264642d6a1"
        const val REDIRECT_URI = "https://accounts.firefox.com/oauth/success/$CLIENT_ID"

        private fun newInstanceDefaultAccountManager(context: Context): FxaAccountManager {
            return FxaAccountManager(
                context,
                Config.release(CLIENT_ID, REDIRECT_URI),
                applicationScopes = APPLICATION_SCOPES,
                deviceConfig = DeviceConfig(
                    name = "Firefox for Fire TV", // TODO: #2516 choose final value.
                    type = DeviceType.MOBILE, // TODO: appservices is considering adding DeviceType.TV.
                    capabilities = setOf(DeviceCapability.SEND_TAB) // required to receive tabs.
                ),
                syncConfig = null
            )
        }
    }
}

/**
 *  A wrapper for [Profile]. This insulates us from any upstream changes to the API, and
 *  allows us to validate related data at the edge of our app.
 *
 *  TODO it looks like some of these fields should be nonnullable (at least [uid] and [email]. We should:
 *  - Reach out to FxA devs for clarification re: which of these fields are actually nullable
 *  - Validate that important fields are nonnull at the edge of our app
 *  - Replace other fields ([avatar], possibly [displayName]) with default values
 */
data class FxaProfile(
    val uid: String?,
    val email: String?,
    val avatar: Avatar?,
    val displayName: String?
)

fun Profile.toDomainObject() = FxaProfile(this.uid, this.email, this.avatar, this.displayName)
