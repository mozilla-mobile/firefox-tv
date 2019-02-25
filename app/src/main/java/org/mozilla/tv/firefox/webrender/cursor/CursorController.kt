/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import android.graphics.PointF
import android.view.KeyEvent
import android.view.View
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.ext.scrollByClamped
import org.mozilla.tv.firefox.webrender.WebRenderFragment

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
class CursorController private constructor(
    // Our lifecycle is shorter than BrowserFragment, so we can hold a reference.
    private val webRenderFragment: WebRenderFragment,
    private val view: CursorView
) : LifecycleObserver {

    private val uiLifecycleCancelJob = Job()

    @Suppress("DEPRECATION") // We are transitioning to CursorViewModel
    private val legacyViewModel = LegacyCursorViewModel(uiLifecycleCancelJob, onUpdate = { x, y, percentMaxScrollVel, framesPassed ->
        view.updatePosition(x, y)
        scrollWebView(percentMaxScrollVel, framesPassed)
    })

    // Initial value does not matter: it will be reactively replaced during start-up.
    lateinit var keyDispatcher: CursorKeyDispatcher

    fun initOnCreateView(viewModel: CursorViewModel, cursorParent: View) {
        cursorParent.addOnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
            legacyViewModel.maxBounds = PointF(right.toFloat(), bottom.toFloat())
        }

        keyDispatcher = CursorKeyDispatcher(isEnabled = false, onDirectionKey = { dir, action ->
            when (action) {
                KeyEvent.ACTION_DOWN -> legacyViewModel.onDirectionKeyDown(dir)
                KeyEvent.ACTION_UP -> legacyViewModel.onDirectionKeyUp(dir)
                else -> Unit
            }
        }, onSelectKey = { event ->
            viewModel.onSelectKeyEvent(event.action, legacyViewModel.pos)
            view.updateCursorPressedState(event)
        })

        viewModel.isEnabled.observe(webRenderFragment.viewLifecycleOwner, Observer { isEnabled ->
            isEnabled ?: return@Observer
            keyDispatcher.isEnabled = isEnabled
            view.visibility = if (isEnabled) View.VISIBLE else View.GONE
        })

        viewModel.touchSimulationLiveData.observe(webRenderFragment.viewLifecycleOwner, Observer {
            it?.consume {
                webRenderFragment.activity?.dispatchTouchEvent(it)
                true
            }
        })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        view.cancelUpdates()
        legacyViewModel.cancelUpdates()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        view.startUpdates()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        uiLifecycleCancelJob.cancel()
    }

    private fun scrollWebView(percentMaxScrollVel: PointF, framesPassed: Float) {
        fun getDeltaScrollAdjustedForTime(percentScrollVel: Float) =
                Math.round(percentScrollVel * MAX_SCROLL_VELOCITY * framesPassed)

        // Adjusting for time guarantees the distance scrolled
        // is the same, even if the framerate drops.
        val scrollX = getDeltaScrollAdjustedForTime(percentMaxScrollVel.x)
        val scrollY = getDeltaScrollAdjustedForTime(percentMaxScrollVel.y)

        webRenderFragment.engineView?.scrollByClamped(scrollX, scrollY)
    }

    companion object {
        fun newInstanceOnCreateView(
            webRenderFragment: WebRenderFragment,
            cursorParent: View,
            view: CursorView,
            viewModel: CursorViewModel
        ): CursorController {
            return CursorController(webRenderFragment, view).apply {
                initOnCreateView(viewModel, cursorParent)
            }
        }
    }
}
