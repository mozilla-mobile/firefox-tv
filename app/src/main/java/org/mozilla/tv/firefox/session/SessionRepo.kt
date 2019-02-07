/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.annotation.AnyThread
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.tv.firefox.ext.postIfNew
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.utils.TurboMode
import org.mozilla.tv.firefox.webrender.EngineViewCache

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
        val currentUrl: String
    )

    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state

    var canGoBackTwice: (() -> Boolean?)? = null
    private var previousURLHost: String? = null

    fun observeSources() {
        SessionObserverHelper.attach(this, sessionManager)
        turboMode.observable.observeForever { update() }
    }

    @AnyThread
    fun update() {
        session?.let { session ->
            fun isHostDifferentFromPrevious(): Boolean {
                val currentURLHost = session.url.toUri()?.host ?: return true

                return (previousURLHost != currentURLHost).also {
                    previousURLHost = currentURLHost
                }
            }
            fun disableDesktopMode() {
                setDesktopMode(false)
                session.url.toUri()?.let { loadURL(it) }
            }
            fun causeSideEffects() {
                if (isHostDifferentFromPrevious() && session.desktopMode) {
                    disableDesktopMode()
                }
            }

            causeSideEffects()

            val newState = State(
                // The menu back button should not be enabled if the previous screen was our initial url (home)
                backEnabled = canGoBackTwice?.invoke() ?: false,
                forwardEnabled = session.canGoForward,
                desktopModeActive = session.desktopMode,
                turboModeActive = turboMode.isEnabled,
                currentUrl = session.url
            )
            _state.postIfNew(newState)
        }
    }

    fun currentURLScreenshot(): Bitmap? = session?.thumbnail

    fun exitFullScreenIfPossibleAndBack() {
        exitFullScreenIfPossible()
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

    fun setTurboModeEnabled(enabled: Boolean) {
        turboMode.isEnabled = enabled
    }

    private val session: Session? get() = sessionManager.selectedSession

    fun clearBrowsingData(context: Context, engineViewCache: EngineViewCache) {
        sessionManager.removeAll()
        engineViewCache.doNotPersist()
    }

    /**
     * Returns true if fullscreen was exited
     */
    fun exitFullScreenIfPossible(): Boolean {
        if (session?.fullScreenMode == true) {
            // Changing the URL while full-screened can lead to unstable behavior
            // (see #1224 and #1719), so we always attempt to exit full-screen
            // before doing so
            sessionManager.getEngineSession()?.exitFullScreenMode()
            return true
        }
        return false
    }
}
