/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import org.mozilla.tv.firefox.R

class NavigationOverlayBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<LinearLayout>(context, attrs) {
    var toolbarHeight = 0

    override fun onLayoutChild(parent: CoordinatorLayout, child: LinearLayout, layoutDirection: Int): Boolean {
        toolbarHeight = child.measuredHeight
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: LinearLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return true
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: LinearLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (target is NestedScrollView) {
            val overlayBottomContainer = (target.findViewById<LinearLayout>(R.id.overlayBottomHalf))
            val containerPosArray = intArrayOf(0, 0)
            overlayBottomContainer.getLocationOnScreen(containerPosArray)

            val containerTop = containerPosArray[1]
            if (containerTop < toolbarHeight) {
                child.y = 1.0f * (containerTop - toolbarHeight)
            } else {
                child.y = 0f
            }
        }
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
    }
}
