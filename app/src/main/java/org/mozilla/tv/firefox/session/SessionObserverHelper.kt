/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager

/**
 * A facade to simplify the process of observing [Session]s and sending their information to a [SessionRepo].
 *
 * Whenever [Session]s are added or removed, this adds/removes a session observer.
 *
 * Whenever [Session]s change, this prompts the [SessionRepo] to update.
 */
class SessionObserverHelper private constructor(sessionRepo: SessionRepo) {

    companion object {
        fun attach(sessionRepo: SessionRepo, sessionManager: SessionManager) {
            val updater = SessionObserverHelper(sessionRepo)
            sessionManager.selectedSession?.register(updater.sessionObserver)
            sessionManager.register(updater.sessionManagerObserver)
        }
    }

    // Any time the observed session changes, force the repo to update
    private val sessionObserver = object : Session.Observer {
        override fun onUrlChanged(session: Session, url: String) {
            sessionRepo.update()
        }

        override fun onDesktopModeChanged(session: Session, enabled: Boolean) {
            sessionRepo.update()
        }

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            sessionRepo.update()
        }

        override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) {
            sessionRepo.update()
        }
    }

    // Any time a new session is created, add a sessionObserver to it.
    // When a session is removed, remove the sessionObserver.
    val sessionManagerObserver = object : SessionManager.Observer {
        override fun onSessionSelected(session: Session) {
            session.register(sessionObserver)
            sessionRepo.update()
        }

        override fun onSessionRemoved(session: Session) {
            session.unregister(sessionObserver)
        }
    }
}
