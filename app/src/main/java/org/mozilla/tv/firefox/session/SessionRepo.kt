/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.Bitmap
import android.support.annotation.AnyThread
import mozilla.components.browser.session.SessionManager
import org.mozilla.tv.firefox.ext.postIfNew

/**
 * TODO
 */
class SessionRepo(private val sessionManager: SessionManager) {

    data class State(
        val backEnabled: Boolean,
        val forwardEnabled: Boolean,
        val desktopModeActive: Boolean,
        val currentUrl: String
    )

    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state

    fun observeSession() {
        SessionObservationManager.attach(this, sessionManager)
    }

    @AnyThread
    fun update() {
        sessionManager.selectedSession?.let {
            val newState = State(
                backEnabled = it.canGoBack,
                forwardEnabled = it.canGoForward,
                desktopModeActive = it.desktopMode,
                currentUrl = it.url
            )
            _state.postIfNew(newState)
        }
    }

    fun currentURLScreenshot(): Bitmap? = sessionManager.selectedSession?.thumbnail
}
