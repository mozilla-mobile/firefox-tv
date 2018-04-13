/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_pocket_video.view.*
import org.mozilla.focus.R

/** A feed of Pocket videos. */
class PocketVideoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_pocket_video, container, false)
        layout.videoFeed.gridView.adapter = PocketVideoAdapter()
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

private class PocketVideoAdapter : RecyclerView.Adapter<PocketVideoViewHolder>() {

    private val feedItems = List(8) { PocketVideo("Mirror-Polished Japanese $it Ball Challenge Crushed", "youtube.com/tv", "youtube.com/tv") }

    override fun getItemCount() = feedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PocketVideoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pocket_video_item, parent, false))

    override fun onBindViewHolder(holder: PocketVideoViewHolder, position: Int) = with (holder) {
        val item = feedItems[position]
        titleView.text = item.title
        subdomainView.text = "youtube" // TODO: get from Video.
        videoThumbnailView.setBackgroundColor(Color.parseColor("#ee0000")) // TODO: load async.
    }
}

private class PocketVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val videoThumbnailView = itemView.findViewById<ImageView>(R.id.videoThumbnailView)
    val titleView = itemView.findViewById<TextView>(R.id.titleView)
    val subdomainView = itemView.findViewById<TextView>(R.id.subdomainView)
}
