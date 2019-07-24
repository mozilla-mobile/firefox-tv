/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Deferred
import mozilla.appservices.fxaclient.Config
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.DeviceCapability
import mozilla.components.concept.sync.DeviceEvent
import mozilla.components.concept.sync.DeviceEventsObserver
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.DeviceConfig
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AUTHENTICATED_NO_PROFILE
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AUTHENTICATED_WITH_PROFILE
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NEEDS_REAUTHENTICATION
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NOT_AUTHENTICATED

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

    enum class AccountState {
        // TODO: Later, may need "failed to login": https://github.com/mozilla-mobile/android-components/issues/3712
        AUTHENTICATED_WITH_PROFILE, // After the profile is fetched async
        AUTHENTICATED_NO_PROFILE, // Before the profile is fetched async
        NEEDS_REAUTHENTICATION,
        NOT_AUTHENTICATED // Initial state
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    val accountObserver = FirefoxAccountObserver()

    private val _accountState: BehaviorSubject<AccountState> = BehaviorSubject.createDefault(NOT_AUTHENTICATED)
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

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    inner class FirefoxAccountObserver : AccountObserver {
        /**
         * The account profile is fetched asynchronously.
         * There is no way to transition from [NOT_AUTHENTICATED] directly to [AUTHENTICATED_WITH_PROFILE];
         * [AUTHENTICATED_NO_PROFILE] is always a state in between.
         */
        override fun onAuthenticated(account: OAuthAccount) {
            logger.debug("onAuthenticated")
            _accountState.onNext(AUTHENTICATED_NO_PROFILE)
        }

        override fun onAuthenticationProblems() {
            logger.debug("onAuthenticationProblems")
            _accountState.onNext(NEEDS_REAUTHENTICATION)
        }

        override fun onLoggedOut() {
            logger.debug("onLoggedOut")
            _accountState.onNext(NOT_AUTHENTICATED)
        }

        /**
         * This is called when the profile is first fetched after sign-in.
         */
        override fun onProfileUpdated(profile: Profile) {
            logger.debug("onProfileUpdated")
            _accountState.onNext(AUTHENTICATED_WITH_PROFILE)
        }
    }

    private inner class FirefoxDeviceEventsObserver : DeviceEventsObserver {
        override fun onEvents(events: List<DeviceEvent>) {
            logger.debug("received device events: $events")
        }
    }

    companion object {
        // TODO #2506: use production FFTV client ID (this ID is from an FxA sample app).
        private const val CLIENT_ID = "a2270f727f45f648"
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
