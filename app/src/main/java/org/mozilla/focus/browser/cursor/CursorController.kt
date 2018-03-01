/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.cursor

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.PointF
import android.view.View
import android.view.accessibility.AccessibilityManager
import kotlinx.android.synthetic.main.fragment_browser.*
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.ext.getAccessibilityManager
import org.mozilla.focus.ext.isVoiceViewEnabled
import kotlin.properties.Delegates

private const val SCROLL_MULTIPLIER = 45

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
        private val browserFragment: BrowserFragment,
        cursorParent: View,
        private val view: CursorView
) : AccessibilityManager.TouchExplorationStateChangeListener, LifecycleObserver {

    private var isEnabled: Boolean by Delegates.observable(true) { _, _, newValue ->
        keyDispatcher.isEnabled = newValue
        view.visibility = if (newValue) View.VISIBLE else View.GONE
    }

    private val viewModel = CursorViewModel(onUpdate = { x, y, scrollVel ->
        view.updatePosition(x, y)
        scrollWebView(scrollVel)
    }, simulateTouchEvent = { browserFragment.activity.dispatchTouchEvent(it) })

    val keyDispatcher = CursorKeyDispatcher(isEnabled, viewModel)

    private val isLoadingObserver = CursorIsLoadingObserver()

    init {
        cursorParent.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            viewModel.maxBounds = PointF(right.toFloat(), bottom.toFloat())
        }
    }

    /** Gets the current state of the browser and updates the cursor enabled state accordingly. */
    fun setEnabledForCurrentState() {
        // These sources have their own navigation controls.
        isEnabled = !browserFragment.webView!!.isYoutubeTV && !browserFragment.context.isVoiceViewEnabled()
                && !browserFragment.browserOverlay.isVisible
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        browserFragment.context.getAccessibilityManager().addTouchExplorationStateChangeListener(this)
        setEnabledForCurrentState() // VoiceView state may change.

        browserFragment.session.loading.observe(browserFragment, isLoadingObserver)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        browserFragment.context.getAccessibilityManager().removeTouchExplorationStateChangeListener(this)

        browserFragment.session.loading.removeObserver(isLoadingObserver)
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

    override fun onTouchExplorationStateChanged(isEnabled: Boolean) {
        setEnabledForCurrentState()
    }

    private fun scrollWebView(scrollVel: PointF) {
        val scrollX = Math.round(scrollVel.x * SCROLL_MULTIPLIER)
        val scrollY = Math.round(scrollVel.y * SCROLL_MULTIPLIER)
        browserFragment.webView?.flingScroll(scrollX, scrollY)
    }

    private inner class CursorIsLoadingObserver : NonNullObserver<Boolean>() {
        override fun onValueChanged(isLoading: Boolean) {
            setEnabledForCurrentState()
        }
    }
}