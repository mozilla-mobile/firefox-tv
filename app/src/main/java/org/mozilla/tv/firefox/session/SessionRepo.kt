/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.Bitmap
import android.net.Uri
import android.support.annotation.AnyThread
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.tv.firefox.ext.postIfNew
import org.mozilla.tv.firefox.utils.TurboMode

/**
 * Repository that is responsible for storing state related to the browser.
 */
class SessionRepo(
    private val sessionManager: SessionManager,
    private val sessionUseCases: SessionUseCases,
    private val turboMode: TurboMode
) {

    data class State(
        val backEnabled: Boolean,
        val forwardEnabled: Boolean,
        val desktopModeActive: Boolean,
        val turboModeActive: Boolean,
        val currentUrl: String,
        val currentBackForwardIndex: Int
    )

    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state

    var backForwardIndexProvider: (() -> Int)? = null

    fun observeSources() {
        SessionObserverHelper.attach(this, sessionManager)
        turboMode.observable.observeForever { update() }
    }

    @AnyThread
    fun update() {
        session?.let {
            val newState = State(
                backEnabled = it.canGoBack,
                forwardEnabled = it.canGoForward,
                desktopModeActive = it.desktopMode,
                turboModeActive = turboMode.isEnabled(),
                currentUrl = it.url,
                currentBackForwardIndex = backForwardIndexProvider?.invoke() ?: -1
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

    fun goForward() {
        if (session?.canGoForward == true) sessionUseCases.goForward.invoke()
    }

    fun reload() = sessionUseCases.reload.invoke()

    fun setDesktopMode(active: Boolean) = session?.let { it.desktopMode = active }

    /**
     * Causes [state] to emit its most recently pushed value. This can be used
     * to reset UI that has been adjusted by the user (e.g., EditText text)
     */
    fun pushCurrentValue() = _state.postValue(_state.value)

    fun loadURL(url: Uri) = session?.let { sessionManager.getEngineSession(it)?.loadUrl(url.toString()) }

    fun setTurboModeEnabled(enabled: Boolean) = turboMode.setEnabled(enabled)

    private val session: Session? get() = sessionManager.selectedSession
}
