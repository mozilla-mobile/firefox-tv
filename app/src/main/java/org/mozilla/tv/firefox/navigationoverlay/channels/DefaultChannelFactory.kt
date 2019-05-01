// todo: license

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.default_channel.view.channelTileContainer
import kotlinx.android.synthetic.main.default_channel.view.channelTitle
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channel.ChannelTile

class DefaultChannelFactory {

    fun createChannel(context: Context, parent: ViewGroup): Channel {
        val channelAdapter = DefaultChannelAdapter()


        val containerView = LayoutInflater.from(context).inflate(R.layout.default_channel, parent, false) as ViewGroup
        containerView.channelTileContainer.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = channelAdapter
        }

        return Channel(
                containerView = containerView,
                adapter = channelAdapter
        )
    }

}

class Channel(  // todo: should be defaultChannel? b/c adapter needs access to `setTiles`
        val containerView: ViewGroup,
        private val adapter: DefaultChannelAdapter
) {

    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    fun setContents(tileData: List<ChannelTile>) {
        adapter.setTiles(tileData)
    }

    val titleView: TextView = containerView.channelTitle
    val tileContainer: RecyclerView = containerView.channelTileContainer
}
