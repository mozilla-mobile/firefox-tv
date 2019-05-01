// todo: license

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

    fun createChannel(context: Context, parent: ViewGroup, id: Int): DefaultChannel {
        // If we ever have channels that don't support removal, add a shouldRemove param here.
        // When false, pass null instead of invokeLongClickAndSaveTile
        val channelAdapter = DefaultChannelAdapter(loadUrl, invokeLongClickAndSaveTile, onTileFocused)

        val containerView = LayoutInflater.from(context).inflate(R.layout.default_channel, parent, false) as ViewGroup
        containerView.channelTileContainer.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = channelAdapter
        }
        containerView.id = id

        return DefaultChannel(
                containerView = containerView,
                adapter = channelAdapter
        )
    }

}
