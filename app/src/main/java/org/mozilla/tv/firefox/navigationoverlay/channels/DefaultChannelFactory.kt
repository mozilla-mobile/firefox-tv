/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.default_channel.view.channelTileContainer
import org.mozilla.tv.firefox.R

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

    fun createChannel(context: Context, parent: ViewGroup, id: Int? = null, shouldRemove: Boolean = true): DefaultChannel {
        val channelAdapter = if (shouldRemove)
            DefaultChannelAdapter(loadUrl, invokeLongClickAndSaveTile, onTileFocused)
        else
            DefaultChannelAdapter(loadUrl, null, onTileFocused)

        val containerView = LayoutInflater.from(context).inflate(R.layout.default_channel, parent, false) as ViewGroup
        containerView.channelTileContainer.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = channelAdapter
        }
        if (id != null) containerView.id = id

        return DefaultChannel(
                channelContainer = containerView,
                adapter = channelAdapter
        )
    }
}
