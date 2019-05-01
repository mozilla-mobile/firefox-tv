// todo: license

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.default_channel.view.channelTileContainer
import kotlinx.android.synthetic.main.default_channel.view.channelTitle
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channel.ChannelTile

class DefaultChannelFactory(
        private val loadUrl: (String) -> Unit,
        onTileLongClick: (() -> Unit),
        val onTileFocused: (() -> Unit)
) {
    val invokeAndSaveLongClick = { tile: ChannelTile ->
        lastLongClickedTile = tile
        onTileLongClick.invoke()
    }

    var lastLongClickedTile: ChannelTile? = null
        private set

    fun createChannel(context: Context, parent: ViewGroup, id: Int): Channel {
        val channelAdapter = DefaultChannelAdapter(loadUrl, invokeAndSaveLongClick, onTileFocused)


        val containerView = LayoutInflater.from(context).inflate(R.layout.default_channel, parent, false) as ViewGroup
        containerView.channelTileContainer.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = channelAdapter
        }
        containerView.id = id

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

    private val titleView: TextView = containerView.channelTitle
//    val tileContainer: RecyclerView = containerView.channelTileContainer
}
