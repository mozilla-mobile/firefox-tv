/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_pocket_video.view.*
import org.mozilla.focus.R
import org.mozilla.focus.ext.updateLayoutParams

/** A feed of Pocket videos. */
class PocketVideoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_pocket_video, container, false)
        layout.videoFeed.gridView.adapter = PocketVideoAdapter(context)

        // SVGs can have artifacts if we set them in XML so we set it in code.
        layout.pocketWordmarkView.setImageDrawable(context.getDrawable(R.drawable.ic_pocket_and_wordmark).apply {
            setTint(ContextCompat.getColor(context, R.color.photonGrey10))
        })
        return layout
    }

    companion object {
        fun create() = PocketVideoFragment()
    }
}

private class PocketVideo(
        val title: String,
        val url: String,
        val imageUrl: String
)

private class PocketVideoAdapter(context: Context) : RecyclerView.Adapter<PocketVideoViewHolder>() {

    // We cache these colors because we swap between them often.
    private val photonGrey90 = ContextCompat.getColor(context, R.color.photonGrey90)
    private val photonGrey70 = ContextCompat.getColor(context, R.color.photonGrey70)
    private val photonGrey50 = ContextCompat.getColor(context, R.color.photonGrey50)
    private val photonGrey10 = ContextCompat.getColor(context, R.color.photonGrey10)
    private val photonGrey10_a99 = ContextCompat.getColor(context, R.color.photonGrey10_a99)
    private val photonGrey10_aCC = ContextCompat.getColor(context, R.color.photonGrey10_aCC)

    private val videoItemHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_video_item_horizontal_margin)
    private val feedHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_feed_horizontal_margin)

    private val feedItems = List(8) { PocketVideo("Mirror-Polished Japanese $it Ball Challenge Crushed", "youtube.com/tv", "youtube.com/tv") }

    override fun getItemCount() = feedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PocketVideoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pocket_video_item, parent, false))

    override fun onBindViewHolder(holder: PocketVideoViewHolder, position: Int) = with (holder) {
        val item = feedItems[position]
        setHorizontalMargins(holder, position)

        holder.itemView.setOnFocusChangeListener { _, hasFocus -> updateForFocusState(holder, hasFocus) }
        updateForFocusState(holder, holder.itemView.isFocused)

        titleView.text = item.title
        subdomainView.text = "youtube" // TODO: get from Video.
        videoThumbnailView.setBackgroundColor(Color.parseColor("#ee0000")) // TODO: load async.
    }

    private fun updateForFocusState(holder: PocketVideoViewHolder, isFocused: Boolean) {
        val titleTextColor: Int
        val subdomainTextColor: Int
        val cardBackground: Int
        if (isFocused) {
            titleTextColor = photonGrey90
            subdomainTextColor = photonGrey50
            cardBackground = photonGrey10
        } else {
            titleTextColor = photonGrey10_aCC
            subdomainTextColor = photonGrey10_a99
            cardBackground = photonGrey70
        }

        with (holder) {
            titleView.setBackgroundColor(cardBackground)
            subdomainView.setBackgroundColor(cardBackground)

            titleView.setTextColor(titleTextColor)
            subdomainView.setTextColor(subdomainTextColor)
        }
    }

    /**
     * Set the horizontal margins on the given view.
     *
     * We want to add padding to the beginning and end of the RecyclerView: ideally we'd just add
     * paddingStart/End. Unfortunately, this causes a visual glitch as each card scrolls offscreen.
     * Instead, we set the margins for the first and last card.
     */
    private fun setHorizontalMargins(holder: PocketVideoViewHolder, position: Int) = holder.itemView.updateLayoutParams {
        (it as ViewGroup.MarginLayoutParams).apply {
            // We need to reset margins on every view, not just first/last, because the View instance can be re-used.
            marginStart = if (position == 0) feedHorizontalMargin else videoItemHorizontalMargin
            marginEnd = if (position == feedItems.size - 1) feedHorizontalMargin else videoItemHorizontalMargin
        }
    }
}

private class PocketVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val videoThumbnailView = itemView.findViewById<ImageView>(R.id.videoThumbnailView)
    val titleView = itemView.findViewById<TextView>(R.id.titleView)
    val subdomainView = itemView.findViewById<TextView>(R.id.subdomainView)
}
