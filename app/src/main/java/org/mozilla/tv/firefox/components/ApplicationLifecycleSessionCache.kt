/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.components

import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager

/**
 * An object that caches [Session]s in place of the [SessionManager] after the
 * [mozilla.components.concept.engine.EngineView] owner's lifecycle has ended and restores the sessions when the
 * EngineView is reattached. This is necessary because Android Components has a memory leak where [Session]s will keep a
 * reference to their system WebView. They null the WebView reference when [Session]s are removed from the
 * [SessionManager] so, as a workaround, we manually remove the [Session]s at the end of the lifecycle and re-add them
 * when a new lifecycle begins.
 *
 * See more details in the issue mentioned below:
 * TODO: implement the components solution in android-components#1915, remove this class and its calls
 *
 * We don't use lifecycle observers in this class because each method must be called at a precise time (see each
 * lifecycle method's kdoc for details).
 */
class ApplicationLifecycleSessionCache(
    private val sessionManager: SessionManager
) {

    @VisibleForTesting(otherwise = PRIVATE) val cachedSessions = mutableListOf<Session>()

    /**
     * Restores the sessions into the [SessionManager] in onCreate if the Activity has been destroyed and recreated but
     * the Application remains: this must be called before the Activity attempts to access the current session.
     */
    fun onCreateMaybeRestoreSessions() {
        if (cachedSessions.isEmpty()) return // unnecessary check but helps clarity.

        cachedSessions.forEach {
            sessionManager.add(it)
        }

        cachedSessions.clear()
    }

    /**
     * Caches the sessions into the cache and removes all sessions from the [SessionManager] in onDestroy: see class kdoc
     * for a greater explanation. afaict, onDestroy will not be called for process death (in which case, this memory
     * leak doesn't matter) but is otherwise guaranteed to be called when the Activity is finished: this should be
     * sufficient for our purposes. This should be called last in onDestroy, just in case another method tries to use
     * the SessionManager
     */
    fun onDestroyCacheSessions() {
        cachedSessions.addAll(sessionManager.all)
        sessionManager.removeAll()
    }
}
