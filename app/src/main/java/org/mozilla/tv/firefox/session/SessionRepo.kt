/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveDataReactiveStreams
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.tv.firefox.ext.isYoutubeTV
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
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

    enum class Event {
        YouTubeBack, ExitYouTube
    }

    private val _state: BehaviorSubject<State> = BehaviorSubject.create()
    val state: Observable<State> = _state.hide()
    @Deprecated(message = "Use SessionRepo.state for new code")
    val legacyState = LiveDataReactiveStreams
            .fromPublisher(state.toFlowable(BackpressureStrategy.LATEST))

    private val _events: Subject<Event> = PublishSubject.create()
    val events = _events.hide()

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

            fun <T> BehaviorSubject<T>.onNextIfNew(value: T) {
                if (this.value != value) this.onNext(value)
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
            _state.onNextIfNew(newState)
        }
    }

    fun currentURLScreenshot(): Bitmap? = session?.thumbnail

    /**
     * @param forceYouTubeExit if true while YouTube is active, back out of the
     * site instead of moving focus
     *
     * @Returns true if the event was consumed
     */
    fun attemptBack(forceYouTubeExit: Boolean = false): Boolean {
        val session = session ?: return false
        if (session.isYoutubeTV && forceYouTubeExit) {
            _events.onNext(Event.ExitYouTube)
            return true
        }

        if (session.isYoutubeTV && !forceYouTubeExit) {
            _events.onNext(Event.YouTubeBack)
            return true
        }

        if (session.canGoBack) {
            exitFullScreenIfPossible()
            sessionUseCases.goBack.invoke()
            TelemetryIntegration.INSTANCE.browserBackControllerEvent()
            return true
        }

        return false
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
    fun pushCurrentValue() = _state.onNext(_state.value!!) // TODO does this do anything? If not,
    // we can have state.distinctUntilChanged and get rid of postIfNew

    fun loadURL(url: Uri) = session?.let { sessionManager.getEngineSession(it)?.loadUrl(url.toString()) }

    fun setTurboModeEnabled(enabled: Boolean) {
        turboMode.isEnabled = enabled
    }

    private val session: Session? get() = sessionManager.selectedSession

    fun clearBrowsingData(engineViewCache: EngineViewCache) {
        sessionManager.getEngineSession()?.clearData() // Only works for [SystemEngineView]
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
