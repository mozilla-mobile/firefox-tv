/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
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
import mozilla.components.service.fxa.manager.DeviceTuple
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AUTHENTICATED
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.AUTHENTICATED_NO_PROFILE
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NEEDS_REAUTHENTICATION
import org.mozilla.tv.firefox.fxa.FxaRepo.AccountState.NOT_AUTHENTICATED

private val logger = Logger("FxaRepo")

private val APPLICATION_SCOPES = arrayOf(
    // We don't use sync, however, if we later add the sync scope, already authenticated users
    // will have to log in again. To avoid this in the future, we preemptively declare a sync scope.
    "https://identity.mozilla.com/apps/oldsync"
)

/**
 * Manages Firefox Account (FxA) state. In particular, [accountStateDontUseMeYet] exposes the current sign in
 * state of the user. If you want to initiate sign in, see [FxaLoginUseCase.beginLogin].
 *
 * Devs should use this class rather than interacting with the FxA library directly.
 */
class FxaRepo(
    context: Context,
    val accountManager: FxaAccountManager = newInstanceDefaultAccountManager(context)
) {

    enum class AccountState {
        // TODO: Are these states accurate?
        // TODO: Later, may need "failed to login": https://github.com/mozilla-mobile/android-components/issues/3712
        AUTHENTICATED,
        AUTHENTICATED_NO_PROFILE, // todo: necessary? profile seems to be fetched async so yes?
        NEEDS_REAUTHENTICATION,
        NOT_AUTHENTICATED
        // TODO: do we need initial state before state is fetched from disk?
    }

    // TODO: set state accurately, choose correct subject, set initial state, etc.
    val accountStateDontUseMeYet = BehaviorSubject.create<AccountState>()

    init {
        accountManager.register(FirefoxAccountObserver())
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

    private inner class FirefoxAccountObserver : AccountObserver {
        override fun onAuthenticated(account: OAuthAccount) {
            logger.debug("onAuthenticated")
            // todo: is this correct?
            val nextState = if (accountManager.accountProfile() != null) {
                AUTHENTICATED
            } else {
                AUTHENTICATED_NO_PROFILE
            }
            accountStateDontUseMeYet.onNext(nextState)
        }

        override fun onAuthenticationProblems() {
            logger.debug("onAuthenticationProblems")
            accountStateDontUseMeYet.onNext(NEEDS_REAUTHENTICATION)
        }

        override fun onError(error: Exception) {
            // This is for internal errors in the sync library and is an unexpected state.
            logger.debug("onError")
        }

        override fun onLoggedOut() {
            logger.debug("onLoggedOut")
            accountStateDontUseMeYet.onNext(NOT_AUTHENTICATED)
        }

        override fun onProfileUpdated(profile: Profile) {
            logger.debug("onProfileUpdated")
            // todo: update state.
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
                deviceTuple = DeviceTuple(
                    name = "Firefox for Fire TV", // TODO: #2516 choose final value.
                    type = DeviceType.MOBILE, // TODO: appservices is considering adding DeviceType.TV.
                    capabilities = listOf(DeviceCapability.SEND_TAB) // required to receive tabs.
                )
            )
        }
    }
}
