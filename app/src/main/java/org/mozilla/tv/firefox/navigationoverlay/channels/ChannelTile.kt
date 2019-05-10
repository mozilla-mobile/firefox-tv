/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.getDimenPixelSize

enum class TileSource { BUNDLED, CUSTOM }

/**
 * Backing data for a [RecyclerView] item in a channel
 */
data class ChannelTile(
    val url: String,
    val title: String,
    val setImage: (ImageView) -> Unit,
    val tileSource: TileSource,
    val id: String,
    val type: TileType
) {
    companion object {
        /**
         * Set the horizontal margins on the given view.
         *
         * We want to add padding to the beginning and end of the RecyclerView: ideally we'd just add
         * paddingStart/End. Unfortunately, this causes a visual glitch as each card scrolls offscreen.
         * Instead, we set the margins for the first and last card.
         *
         * See [DefaultChannelAdapter.onBindViewHolder] for an example
         */
        fun setChannelMarginByPosition(view: View, context: Context, position: Int, itemCount: Int) {
            val defaultItemHorizontalMargin = context.getDimenPixelSize(R.dimen.pocket_video_item_horizontal_margin)
            val overlayMarginStart = context.getDimenPixelSize(R.dimen.overlay_margin_start)
            val overlayMarginEnd = context.getDimenPixelSize(R.dimen.overlay_margin_end)

            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // We need to reset margins on every view, not just first/last, because the View instance can be re-used.
                marginStart = if (position == 0) overlayMarginStart else defaultItemHorizontalMargin
                marginEnd = if (position == itemCount - 1) overlayMarginEnd else defaultItemHorizontalMargin
            }
        }
    }
}

/**
 * Backing data for a channel as a whole
 */
data class ChannelDetails(
    val title: CharSequence,
    val tileList: List<ChannelTile>
)
