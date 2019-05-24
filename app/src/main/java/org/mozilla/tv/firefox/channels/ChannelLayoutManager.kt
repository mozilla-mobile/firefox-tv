/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.getDimenPixelSize

const val MILLISECONDS_PER_INCH = 50f // For smooth scrolling speed

/**
 * [ChannelLayoutManager] manages scrolling state of the channel RecyclerView while satisfying
 * SNAP_SCROLL behaviour
 */
class ChannelLayoutManager(
    private val context: Context
) : LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {

    @Volatile private var isScrolling: Boolean = false

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

    override fun onScrollStateChanged(state: Int) {
        isScrolling = when (state) {
            SCROLL_STATE_SETTLING -> true
            else -> false
        }

        super.onScrollStateChanged(state)
    }

    override fun onRequestChildFocus(
        parent: RecyclerView,
        state: RecyclerView.State,
        child: View,
        focused: View?
    ): Boolean {
        child.let {
            val pos = getPosition(it)

            // For carousel scrolling margin updates. Precondition here is that the channel
            // RecyclerView has gained focus. Mainly for handling remove tiles (the adjacent tile
            // gains focus upon removal)
            ChannelTile.setChannelMarginByPosition(it, context, pos, itemCount)

            // If removing first item, position can be -1; in which case, don't smoothScro
            if (pos == RecyclerView.NO_POSITION)
                return false

            // Don't call smoothScrollToPosition when ScrollState is in [SCROLL_STATE_SETTLING]
            // Otherwise it causes over throttling in ViewGroup's internal logic to determine
            // nextChildFocus. For first and last element, the updated margin seems to be only
            // redrawn when smoothScrolled
            if (!isScrolling || pos == 0 || pos == state.itemCount - 1)
                smoothScrollToPosition(parent, state, pos)
        }

        return super.onRequestChildFocus(parent, state, child, focused)
    }

    override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
        // By default, moving left when the first element is focused or right when the last element
        // is focused will move focus up or down. This prevents the focus from moving in either case.
        // Code taken from: https://stackoverflow.com/a/51645806/9307461

        // focused is assumed to be the itemView, and will crash if we focus on a subview
        val position = getPosition(focused)
        val firstItemAndLeft = position == 0 && direction == View.FOCUS_LEFT
        val lastItemAndRight = position == itemCount - 1 && direction == View.FOCUS_RIGHT

        return when {
            firstItemAndLeft || lastItemAndRight -> focused
            else -> super.onInterceptFocusSearch(focused, direction)
        }
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = FirstSmoothScroller(context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    /**
     * [FirstSmoothScroller] is designed to support carousel scrolling and scrolling speed.
     * A majority of its logic are derived from its super methods.
     */
    class FirstSmoothScroller(private val context: Context) : LinearSmoothScroller(context) {
        // Scrolling speed; changeable with [MILLISECONDS_PER_INCH]
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            var speed = super.calculateSpeedPerPixel(displayMetrics)
            displayMetrics?.let {
                speed = MILLISECONDS_PER_INCH / it.densityDpi
            }
            return speed
        }

        /**
         * Carousel scrolling - see [LinearSmoothScroller.calculateDxToMakeVisible] and
         * [LinearSmoothScroller.calculateDtToFit] with [LinearSmoothScroller.SNAP_TO_START]
         * preference for the original implementation / reference.
         *
         * If scrolling to an index > 0 and RV is scrollable, set the RecyclerView's marginStart
         * to 0 with [state] (so that the drawable margin increases) and shift the [targetView]
         * by [R.dimen.overlay_margin_channel_start] margin (resulting in partially visible view
         * to the left).
         */
        override fun calculateDxToMakeVisible(targetView: View, snapPreference: Int): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0
            }
            val params = targetView.layoutParams as RecyclerView.LayoutParams
            val left = layoutManager.getDecoratedLeft(targetView) - params.marginStart
            val start = context.getDimenPixelSize(R.dimen.overlay_margin_channel_start)

            return start - left
        }
    }
}
