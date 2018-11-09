/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.Bitmap
import android.support.annotation.AnyThread
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.tv.firefox.ext.postIfNew

/**
 * TODO
 */
class SessionRepo(private val sessionManager: SessionManager, private val sessionUseCases: SessionUseCases) {

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
        session?.let {
            val newState = State(
                backEnabled = it.canGoBack,
                forwardEnabled = it.canGoForward,
                desktopModeActive = it.desktopMode,
                currentUrl = it.url
            )
            _state.postIfNew(newState)
        }
    }

    fun currentURLScreenshot(): Bitmap? = session?.thumbnail

    fun exitFullScreenIfPossibleAndBack() {
        // Backing while full-screened can lead to unstable behavior (see #1224),
        // so we always attempt to exit full-screen before backing
        sessionManager.getEngineSession()?.exitFullScreenMode()
        if (session?.canGoBack == true) sessionUseCases.goBack.invoke()
    }

    fun goForward() = if (session?.canGoForward == true) sessionUseCases.goForward.invoke() else { }

    fun reload() = sessionUseCases.reload.invoke()

    fun setDesktopMode(active: Boolean) = session?.let { it.desktopMode = active }

    fun pushCurrentValue() = _state.postValue(_state.value)

    private val session: Session? get() = sessionManager.selectedSession
}
