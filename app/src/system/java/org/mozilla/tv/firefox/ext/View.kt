/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import mozilla.components.browser.engine.system.NestedWebView
import org.mozilla.tv.firefox.R

private const val NESTED_WEB_VIEW_ID = 2147483646 // Int.MAX_VALUE - 1

/**
 * When view gains focus, its child(ren) views may gain focus with undefined View_ID
 * due to programmatic declaration
 */
fun View.validateKnownViewById(): Int {
    if (this.id == View.NO_ID) {
        when (this) {
            is NestedWebView -> return NESTED_WEB_VIEW_ID
            else -> {
                // TODO: need sentry/telemetry to keep track of what views without IDs get passed in
            }
        }
    }

    return this.id
}

/**
 * Set the horizontal margins on the given view.
 *
 * We want to add padding to the beginning and end of the RecyclerView: ideally we'd just add
 * paddingStart/End. Unfortunately, this causes a visual glitch as each card scrolls offscreen.
 * Instead, we set the margins for the first and last card.
 *
 * See [DefaultChannelAdapter.onBindViewHolder] for an example
 */
fun View.setHorizontalMargins(context: Context, position: Int, itemCount: Int) {
    val defaultItemHorizontalMargin = context.getDimenPixelSize(R.dimen.pocket_video_item_horizontal_margin)
    val overlayMarginStart = context.getDimenPixelSize(R.dimen.overlay_margin_start)
    val overlayMarginEnd = context.getDimenPixelSize(R.dimen.overlay_margin_end)

    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        // We need to reset margins on every view, not just first/last, because the View instance can be re-used.
        marginStart = if (position == 0) overlayMarginStart else defaultItemHorizontalMargin
        marginEnd = if (position == itemCount - 1) overlayMarginEnd else defaultItemHorizontalMargin
    }
}

/**
 * Returns false if the view or any of its ancestors are not visible
 */
val View.isEffectivelyVisible: Boolean get() {
    var node: View? = this
    while (node != null) {
        if (node.visibility != View.VISIBLE) return false
        node = node.parent as? View
    }
    return true
}
