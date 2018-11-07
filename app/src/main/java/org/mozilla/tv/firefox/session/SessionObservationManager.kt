/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager

/**
 * TODO
 * when sessions are added or removed, add/remove session observers
 * when sessions change, update repo
 */
class SessionObservationManager private constructor(sessionRepo: SessionRepo) { // TODO rename this

    companion object {
        fun attach(sessionRepo: SessionRepo, sessionManager: SessionManager) {
            val updater = SessionObservationManager(sessionRepo)
            sessionManager.selectedSession?.register(updater.sessionObserver)
            sessionManager.register(updater.sessionManagerObserver)
        }
    }

    // Updates the repo whenever the observed session changes
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

    // Attaches and removes the session observer whenever sessions are added
    // and removed
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
