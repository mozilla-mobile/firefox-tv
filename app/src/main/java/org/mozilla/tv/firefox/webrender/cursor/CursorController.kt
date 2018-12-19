/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.PointF
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.Job
import mozilla.components.browser.session.Session
import org.mozilla.tv.firefox.webrender.WebRenderFragment
import org.mozilla.tv.firefox.ext.getAccessibilityManager
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.isYoutubeTV
import org.mozilla.tv.firefox.ext.scrollByClamped
import kotlin.properties.Delegates

private const val MAX_SCROLL_VELOCITY = 13

/**
 * Encapsulates interactions of the Cursors components. It has the following responsibilities:
 * - Data: references to each Cursor component
 * - Controller: manages interactions between the components and the parent fragment
 * - Lifecycle management: provides lifecycle callbacks; nulling a reference to this controller
 * will also prevent access to the components beyond the lifecycle.
 *
 * For simplicity, the lifecycle of the ViewModel and the KeyDispatcher are the same as the View.
 *
 * When using this class, don't forget to add it as a [LifecycleObserver].
 */
class CursorController(
    // Our lifecycle is shorter than BrowserFragment, so we can hold a reference.
    private val webRenderFragment: WebRenderFragment,
    cursorParent: View,
    private val view: CursorView
) : AccessibilityManager.TouchExplorationStateChangeListener, LifecycleObserver {
    private val uiLifecycleCancelJob = Job()

    private var isEnabled: Boolean by Delegates.observable(true) { _, _, newValue ->
        keyDispatcher.isEnabled = newValue
        view.visibility = if (newValue) View.VISIBLE else View.GONE
    }

    private val viewModel = CursorViewModel(uiLifecycleCancelJob, onUpdate = { x, y, percentMaxScrollVel, framesPassed ->
        view.updatePosition(x, y)
        scrollWebView(percentMaxScrollVel, framesPassed)
    }, simulateTouchEvent = { webRenderFragment.activity?.dispatchTouchEvent(it) })

    val keyDispatcher = CursorKeyDispatcher(isEnabled, onDirectionKey = { dir, action ->
        when (action) {
            KeyEvent.ACTION_DOWN -> viewModel.onDirectionKeyDown(dir)
            KeyEvent.ACTION_UP -> viewModel.onDirectionKeyUp(dir)
            else -> Unit
        }
    }, onSelectKey = { event ->
        viewModel.onSelectKeyEvent(event.action)
        view.updateCursorPressedState(event)
    })

    private val isLoadingObserver = CursorIsLoadingObserver()

    init {
        cursorParent.addOnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
            viewModel.maxBounds = PointF(right.toFloat(), bottom.toFloat())
        }
    }

    /** Gets the current state of the browser and updates the cursor enabled state accordingly. */
    fun setEnabledForCurrentState() {
        // These sources have their own navigation controls.

        isEnabled = !webRenderFragment.session.isYoutubeTV && !(webRenderFragment.context?.isVoiceViewEnabled() ?: false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        webRenderFragment.context?.getAccessibilityManager()?.addTouchExplorationStateChangeListener(this)
        setEnabledForCurrentState() // VoiceView state may change.

        webRenderFragment.session.register(isLoadingObserver, owner = webRenderFragment)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        webRenderFragment.context?.getAccessibilityManager()?.removeTouchExplorationStateChangeListener(this)

        webRenderFragment.session.unregister(isLoadingObserver)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        view.cancelUpdates()
        viewModel.cancelUpdates()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        view.startUpdates()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        uiLifecycleCancelJob.cancel()
    }

    override fun onTouchExplorationStateChanged(isEnabled: Boolean) {
        setEnabledForCurrentState()
    }

    private fun scrollWebView(percentMaxScrollVel: PointF, framesPassed: Float) {
        fun getDeltaScrollAdjustedForTime(percentScrollVel: Float) =
                Math.round(percentScrollVel * MAX_SCROLL_VELOCITY * framesPassed)

        // Adjusting for time guarantees the distance scrolled
        // is the same, even if the framerate drops.
        val scrollX = getDeltaScrollAdjustedForTime(percentMaxScrollVel.x)
        val scrollY = getDeltaScrollAdjustedForTime(percentMaxScrollVel.y)

        webRenderFragment.webView?.scrollByClamped(scrollX, scrollY)
    }

    private inner class CursorIsLoadingObserver : Session.Observer {
        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            setEnabledForCurrentState()
        }
    }
}
