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

const val MILLISECONDS_PER_INCH = 50f // For smooth scrolling speed

class ChannelLayoutManager(
    private val context: Context
) : LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
    override fun onRequestChildFocus(
        parent: RecyclerView,
        state: RecyclerView.State,
        child: View,
        focused: View?
    ): Boolean {
        focused?.let {
            val pos = getPosition(it)

            smoothScrollToPosition(parent, state, pos)
        }

        return super.onRequestChildFocus(parent, state, child, focused)
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val smoothScroller = FirstSmoothScroller(context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    class FirstSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            var speed = super.calculateSpeedPerPixel(displayMetrics)
            displayMetrics?.let {
                speed = MILLISECONDS_PER_INCH / it.densityDpi
            }
            return speed
        }

        override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollHorizontally()) {
                return 0
            }
            val params = view.layoutParams as RecyclerView.LayoutParams
            val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
            val start = layoutManager.paddingLeft
            return start - left
        }
    }
}
