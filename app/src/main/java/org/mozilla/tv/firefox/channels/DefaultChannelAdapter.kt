/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.dialog_channel_tiles.cancelButton
import kotlinx.android.synthetic.main.dialog_channel_tiles.removeTileButton
import kotlinx.android.synthetic.main.dialog_channel_tiles.titleText
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.home_tile.view.channel_cardview
import org.mozilla.tv.firefox.R

val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChannelTile>() {
    override fun areItemsTheSame(oldTile: ChannelTile, newTile: ChannelTile): Boolean {
        return oldTile.url == newTile.url &&
                oldTile.title == newTile.title
    }

    override fun areContentsTheSame(oldTile: ChannelTile, newTile: ChannelTile): Boolean {
        return oldTile.url == newTile.url &&
                oldTile.title == newTile.title
    }
}

class DefaultChannelAdapter(
    private val context: Context,
    private val loadUrl: (String) -> Unit,
    private val onTileFocused: (() -> Unit)?,
    private val channelConfig: ChannelConfig
) : ListAdapter<ChannelTile, DefaultChannelTileViewHolder>(DIFF_CALLBACK) {

    private val _removeEvents: Subject<ChannelTile> = PublishSubject.create<ChannelTile>()
    val removeEvents: Observable<ChannelTile> = _removeEvents.hide()

    private val _focusChangeObservable = PublishSubject.create<Pair<Int, Boolean>>()
    /**
     * Emits upon focus change events.  Sends Pair of tile index to focusGained
     */
    val focusChangeObservable: Observable<Pair<Int, Boolean>> = _focusChangeObservable.hide()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultChannelTileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.home_tile, parent, false)
        return DefaultChannelTileViewHolder(view)
    }

    override fun onBindViewHolder(holder: DefaultChannelTileViewHolder, position: Int) {
        with(holder) {
            // For carousel scrolling margin updates; For the initial pre-focus state. This
            // doesn't unfortunately handle tile removal - which is handled in
            // [ChannelLayoutManager.onRequestChildFocus()]
            ChannelTile.setChannelMarginByPosition(holder.itemView, context, position, itemCount)
            val tile = getItem(position)
            tile.setImage.invoke(imageView)

            titleView.text = tile.title

            itemView.setOnClickListener {
                loadUrl(tile.url)
                channelConfig.onClickTelemetry?.invoke(tile)
            }

            if (channelConfig.itemsMayBeRemoved) {
                setRemoveOnLongClickListener(itemView, tile)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                // We can't use a selector for the tile cardview because we use the focused item to
                // get the RecyclerView adapter position
                val focusRingDrawable: Drawable?
                val animation: StateListAnimator
                if (hasFocus) {
                    focusRingDrawable = context.getDrawable(R.drawable.tile_selected_stroke)
                    animation = AnimatorInflater.loadStateListAnimator(context, R.animator.channel_item_animator_focused)
                    onTileFocused?.invoke()
                } else {
                    focusRingDrawable = null
                    animation = AnimatorInflater.loadStateListAnimator(context, R.animator.channel_item_animator_not_focused)
                }
                itemView.channel_cardview.stateListAnimator = animation
                itemView.channel_cardview.foreground = focusRingDrawable
                _focusChangeObservable.onNext(position to hasFocus)
                channelConfig.onFocusTelemetry?.invoke(tile, hasFocus)
            }
        }
    }

    private fun setRemoveOnLongClickListener(itemView: View, tile: ChannelTile) {
        itemView.setOnLongClickListener {
            channelConfig.onLongClickTelemetry?.invoke(tile)
            val dialog = Dialog(context, R.style.DialogStyle)
            dialog.setContentView(R.layout.dialog_channel_tiles)
            dialog.window?.setDimAmount(0.85f)

            dialog.titleText.text = tile.generateRemoveTileTitleStr(context)
            dialog.removeTileButton.setOnClickListener {
                _removeEvents.onNext(tile)
                dialog.dismiss()
            }

            dialog.cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            true
        }
    }
}

class DefaultChannelTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val titleView: TextView = itemView.findViewById(R.id.tile_title)
    val imageView: ImageView = itemView.findViewById(R.id.tile_icon)
}
