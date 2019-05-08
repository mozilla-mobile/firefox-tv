/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.ext.MARGIN_START_OVERLAY_DP
import org.mozilla.tv.firefox.ext.toPx

const val MILLISECONDS_PER_INCH = 50f // For smooth scrolling speed

/**
 * [ChannelLayoutManager] manages scrolling state of the channel RecyclerView while satisfying
 * SNAP_SCROLL behaviour
 */
class ChannelLayoutManager(
    private val context: Context
) : LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {

    enum class State {
        START,
        SCROLL
    }

    private val _state = BehaviorSubject.createDefault(State.START)
    val state: Observable<State> = _state.hide()
            .distinctUntilChanged()

    /**
     * Android by default, when SNAP_TO_START, attempts to focus the leftmost partially visible
     * descendant (See [findFirstVisibleChildClosestToStart]). Due to carousel scroll, there is
     * a bleeding drawable (partially visible) to the left (to indicate scrollability).
     *
     * [requestDefaultFocus] requests focus on the leftmost "completely" visible item
     */
    fun requestDefaultFocus() {
        findViewByPosition(findFirstCompletelyVisibleItemPosition())?.requestFocus()
    }

    override fun onRequestChildFocus(
        parent: RecyclerView,
        state: RecyclerView.State,
        child: View,
        focused: View?
    ): Boolean {
        focused?.let {
            val pos = getPosition(it)

            when (pos) {
                // Removing first element, don't call smoothScrollToPosition
                RecyclerView.NO_POSITION -> return false
                0 -> _state.onNext(State.START)
                else -> _state.onNext(State.SCROLL)
            }

            smoothScrollToPosition(parent, state, pos)
        }

        return super.onRequestChildFocus(parent, state, child, focused)
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = FirstSmoothScroller(context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    /**
     * [FirstSmoothScroller] is designed to support carousel scrolling and scrolling speed
     */
    class FirstSmoothScroller(private val context: Context) : LinearSmoothScroller(context) {

        // Scrolling speed
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            var speed = super.calculateSpeedPerPixel(displayMetrics)
            displayMetrics?.let {
                speed = MILLISECONDS_PER_INCH / it.densityDpi
            }
            return speed
        }

        // Carousel scrolling
        override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0
            }
            val params = view.layoutParams as RecyclerView.LayoutParams
            val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
            val start = MARGIN_START_OVERLAY_DP.toPx(context)

            return start - left
        }
    }
}
