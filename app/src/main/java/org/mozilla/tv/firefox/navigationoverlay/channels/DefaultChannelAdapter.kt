/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channel.ChannelTile
import org.mozilla.tv.firefox.pinnedtile.PinnedTile
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

class DefaultChannelAdapter(
        private val loadUrl: (String) -> Unit,
        private val onTileLongClick: ((ChannelTile) -> Unit)?,
        private val onTileFocused: (() -> Unit)?
) : RecyclerView.Adapter<DefaultChannelTileViewHolder>() {

    private var tiles: List<ChannelTile> = emptyList()

    fun setTiles(tiles: List<ChannelTile>) {
        // todo; this
        this.tiles = tiles
        notifyDataSetChanged()
        // todo: notify changed; Use the adapter that uses DiffUtil under the hood (in RecyclerView androidx lib?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultChannelTileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
//        val view = inflater.inflate(R.layout.default_channel_tile, parent, false)
        val view = inflater.inflate(R.layout.home_tile, parent, false)
        return DefaultChannelTileViewHolder(view)
    }

    override fun getItemCount(): Int = tiles.size

    override fun onBindViewHolder(holder: DefaultChannelTileViewHolder, position: Int) {
        with(holder) {
            val tile = tiles[position]
            tile.setImage.invoke(imageView)
            titleView.text = tile.title

            itemView.setOnClickListener {
                loadUrl(tile.url)
                // TODO channel telemetry. See TelemetryIntegration#homeTileClickEvent
            }

            itemView.setOnLongClickListener {
                onTileLongClick?.invoke(tile)

                true
            }

            val tvWhiteColor = ContextCompat.getColor(holder.itemView.context, R.color.tv_white)
            itemView.setOnFocusChangeListener { _, hasFocus ->
                val backgroundResource: Int
                val textColor: Int
                if (hasFocus) {
                    backgroundResource = R.drawable.home_tile_title_focused_background
                    textColor = tvWhiteColor
                    onTileFocused?.invoke()
                } else {
                    backgroundResource = 0
                    textColor = Color.BLACK
                }
                titleView.setBackgroundResource(backgroundResource)
                titleView.setTextColor(textColor)
            }

            setIconLayoutMarginParams(imageView, R.dimen.bundled_home_tile_margin_value) // TODO bundled and custom tiles have different padding here!  Ugh!
        }
    }

    private fun setIconLayoutMarginParams(iconView: View, tileMarginValue: Int) {
        val layoutMarginParams = iconView.layoutParams as ViewGroup.MarginLayoutParams
        val marginValue = iconView.resources.getDimensionPixelSize(tileMarginValue)
        layoutMarginParams.setMargins(marginValue, marginValue, marginValue, marginValue)
        iconView.layoutParams = layoutMarginParams
    }

}

class DefaultChannelTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val titleView: TextView = itemView.findViewById(R.id.tile_title)
    val imageView: ImageView = itemView.findViewById(R.id.tile_icon)
}