/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.default_channel.view.channelTileContainer
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.MARGIN_START_OVERLAY_DP
import org.mozilla.tv.firefox.ext.toPx

class DefaultChannelFactory(
    private val loadUrl: (String) -> Unit,
    onTileLongClick: (() -> Unit),
    val onTileFocused: (() -> Unit)
) {
    private val invokeLongClickAndSaveTile = { tile: ChannelTile ->
        lastLongClickedTile = tile
        onTileLongClick.invoke()
    }

    var lastLongClickedTile: ChannelTile? = null
        private set

    fun createChannel(context: Context, parent: ViewGroup, id: Int? = null): DefaultChannel {
        // If we ever have channels that don't support removal, add a shouldRemove param here.
        // When false, pass null instead of invokeLongClickAndSaveTile
        val channelAdapter = DefaultChannelAdapter(loadUrl, invokeLongClickAndSaveTile, onTileFocused)

        val containerView = LayoutInflater.from(context).inflate(R.layout.default_channel, parent, false) as ViewGroup
        containerView.channelTileContainer.apply {
            val channelLayoutManager = ChannelLayoutManager(context)
            val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
            channelLayoutManager.state.subscribe { state ->
                /**
                 * If scrolling, set the start margin to 0 to accommodate carousel scrolling,
                 * default margin [MARGIN_START_OVERLAY_DP] otherwise.
                 * See [ChannelLayoutManager.FirstSmoothScroller.calculateDxToMakeVisible]
                 */
                layoutParams.marginStart = when (state) {
                    ChannelLayoutManager.State.START -> MARGIN_START_OVERLAY_DP.toPx(context)
                    ChannelLayoutManager.State.SCROLL -> 0
                    null -> layoutParams.marginStart
                }

                setLayoutParams(layoutParams)
            }

            layoutManager = channelLayoutManager

            this.adapter = channelAdapter
        }
        if (id != null) containerView.channelTileContainer.id = id

        return DefaultChannel(
                channelContainer = containerView,
                adapter = channelAdapter
        )
    }
}
