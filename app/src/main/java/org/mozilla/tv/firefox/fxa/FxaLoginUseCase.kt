/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.net.Uri
import androidx.fragment.app.FragmentManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AuthType
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.toAuthType
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.telemetry.SentryIntegration

private val logger = Logger("FxaLoginUseCase")

/**
 * Manages login with a Firefox Account (FxA).
 *
 * To begin the login flow, we call [beginLogin]: this loads the login page into the engine view. This login page
 * does the heavy lifting: most error states (e.g. invalid credentials) are handled in the engine view and are not
 * reported to us. On success, our observer in this class transmits the results from the engine view back to the FxA
 * library. After that, control of FxA state is returned to the [fxaRepo].
 *
 * This class is designed to allows repos to interact with each other without references to each other,
 * allowing each repo to operate independently of other repos, simplifying state.
 *
 * FxaRepo    SessionRepo
 *    |            |
 *     ------------
 *           |
 *    FxaLoginUseCase
 */
class FxaLoginUseCase(
    private val fxaRepo: FxaRepo,
    private val sessionRepo: SessionRepo,
    private val screenController: ScreenController,
    private val sentryIntegration: SentryIntegration = SentryIntegration
) {

    private val _onLoginSuccess = PublishSubject.create<Unit>()
    val onLoginSuccess: Observable<Unit> = _onLoginSuccess.hide()

    /**
     * Opens the browser screen and loads the FxA login URL to begin the login flow.
     */
    fun beginLogin(fragmentManager: FragmentManager) {
        // TODO: should we throw an error if we're already authenticated when this is called?
        GlobalScope.launch(Dispatchers.Main) { // main thread: we modify UI state.
            // a-c#3713: this await will never resume if the user is already logged in.
            val loginUri = fxaRepo.beginLoginInternalAsync().await()

            // This may be null when the FxA library fails to do things internally, mostly likely due to network issues.
            if (loginUri == null) {
                sentryIntegration.captureAndLogError(
                    logger, IllegalStateException("beginAuthenticationAsync returned null loginUri"))
                return@launch
            }

            // TODO: if user is already signing in, we should consider not reloading the page.
            screenController.showBrowserScreenForUrl(fragmentManager, loginUri)
        }
    }

    init {
        // @Suppress cannot be used on an initializer so we call a helper method.
        attachFxaLoginSuccessObserver()
    }

    @Suppress("CheckResult") // no need to dispose: sessionRepo is active for the duration of the app.
    private fun attachFxaLoginSuccessObserver() {
        fun isLoginSuccessUri(uri: String): Boolean = uri.startsWith(FxaRepo.REDIRECT_URI)

        fun extractLoginSuccessKeys(uriStr: String): LoginSuccessKeys? {
            val uri = Uri.parse(uriStr)
            val authType = uri.getQueryParameter("action").toAuthType()
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            return if (code != null && state != null) LoginSuccessKeys(authType = authType, code = code, state = state) else null
        }

        fun Observable<String>.filterMapLoginSuccessKeys(): Observable<LoginSuccessKeys> =
            this.flatMap { url ->
                val loginSuccessKeys = extractLoginSuccessKeys(url)

                if (loginSuccessKeys != null) {
                    Observable.just(loginSuccessKeys)
                } else {
                    // Since we received a login success URL, this is never expected. However, since this action is
                    // controlled by a server, we don't want to crash the app so we log to Sentry instead.
                    sentryIntegration.captureAndLogError(
                        logger, IllegalStateException("Received success URI but success keys cannot be found"))
                    Observable.empty()
                }
            }

        sessionRepo.state
            .map { it.currentUrl }
            .distinctUntilChanged()
            .filter { isLoginSuccessUri(it) }
            .filterMapLoginSuccessKeys()
            .subscribe { loginSuccessKeys ->
                fxaRepo.accountManager.finishAuthenticationAsync(loginSuccessKeys)
                _onLoginSuccess.onNext(Unit)
            }
    }

    private data class LoginSuccessKeys(val authType: AuthType, val code: String, val state: String)

    private fun FxaAccountManager.finishAuthenticationAsync(keys: LoginSuccessKeys) {
        @Suppress("DeferredResultUnused") // We don't care to wait until completion.
        finishAuthenticationAsync(FxaAuthData(keys.authType, keys.code, keys.state))
    }
}
