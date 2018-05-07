/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_pocket_video.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.R
import org.mozilla.focus.ScreenController
import org.mozilla.focus.ext.resetAfter
import org.mozilla.focus.ext.updateLayoutParams
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FormattedDomain
import org.mozilla.focus.utils.PicassoWrapper
import java.net.URI
import java.net.URISyntaxException

/** A feed of Pocket videos. */
class PocketVideoFragment : Fragment() {

    private lateinit var deferredVideos: PocketVideosDeferred

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The video feed is typically provided by the creator of this fragment such that the videos
        // on that view (the home screen) match the ones in this feed. However, if this fragment is
        // being reconstructed, the data will no longer be in memory so we get retrieve the data again
        // here. Typically, the data would be provided with `Fragment.getArguments`, which would be
        // provided to reconstructed fragment but we intentionally don't do that: if the feed is
        // being reconstructed, the user probably cares more about an up-to-date feed rather than
        // consistency with the home screen they saw a while ago.
        if (!this::deferredVideos.isInitialized) {
            deferredVideos = Pocket.getRecommendedVideos()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fun displayFeed(layout: View, videos: PocketVideosDeferred) {
            layout.videoFeed.gridView.adapter = PocketVideoAdapter(context, videos, fragmentManager)
        }

        val layout = inflater.inflate(R.layout.fragment_pocket_video, container, false)

        if (deferredVideos.isCompleted) {
            displayFeed(layout, deferredVideos)
        } else {
            // TODO: #769?: display loading screen.

            launch(UI) {
                val videos = deferredVideos.await()
                if (videos != null) {
                    displayFeed(layout, deferredVideos)
                } else {
                    // TODO: #769: display error screen.
                }
            }
        }

        // SVGs can have artifacts if we set them in XML so we set it in code.
        layout.pocketWordmarkView.setImageDrawableAsPocketWordmark()
        return layout
    }

    companion object {
        fun create(videos: PocketVideosDeferred) = PocketVideoFragment().apply {
            // See onCreate for why we don't use `arguments`.
            this.deferredVideos = videos
        }
    }
}

private class PocketVideoAdapter(
        context: Context,
        feedItemsDeferred: PocketVideosDeferred,
        private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<PocketVideoViewHolder>() {

    private val photonGrey70 = ContextCompat.getColor(context, R.color.photonGrey70)
    private val photonGrey60 = ContextCompat.getColor(context, R.color.photonGrey60)
    private val photonGrey10 = ContextCompat.getColor(context, R.color.photonGrey10)
    private val photonGrey10_a80p = ContextCompat.getColor(context, R.color.photonGrey10_a80p)

    private val videoItemHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_video_item_horizontal_margin)
    private val feedHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_feed_horizontal_margin)

    private var feedItems: List<PocketVideo> = if (!feedItemsDeferred.isCompleted) {
        // We let the parent wait for this to complete to simplify the code here.
        throw IllegalStateException("Expected deferred to be completed in parent before creation")
    } else {
        feedItemsDeferred.getCompleted() ?: emptyList()
    }

    override fun getItemCount() = feedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PocketVideoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pocket_video_item, parent, false))

    override fun onBindViewHolder(holder: PocketVideoViewHolder, position: Int) = with (holder) {
        val item = feedItems[position]
        setHorizontalMargins(holder, position)

        holder.itemView.setOnClickListener {
            ScreenController.showBrowserScreenForUrl(fragmentManager, item.url, Source.POCKET_VIDEO_SUGGESTION)
            TelemetryWrapper.pocketVideoClickEvent(item.id)
        }
        holder.itemView.setOnFocusChangeListener { _, hasFocus -> updateForFocusState(holder, hasFocus) }
        updateForFocusState(holder, holder.itemView.isFocused)

        titleView.text = item.title
        PicassoWrapper.client.load(item.thumbnailURL).into(videoThumbnailView)

        @Suppress("TooGenericExceptionCaught") // See below.
        val itemURI = try {
            URI(item.dedupeURL)
        } catch (e: Exception) { // Apparently Kotlin doesn't have multi-catch.
            when (e) {
                is URISyntaxException, is NullPointerException -> null
                else -> throw e
            }
        }
        domainView.text = if (itemURI == null) {
            item.dedupeURL
        } else {
            // The first time this method is called ever, it may block until the file is cached on disk.
            // We pre-cache on startup so I'm hoping this isn't an issue.
            StrictMode.allowThreadDiskReads().resetAfter {
                FormattedDomain.format(holder.itemView.context, itemURI, false, 0)
            }
        }
    }

    private fun updateForFocusState(holder: PocketVideoViewHolder, isFocused: Boolean) {
        val titleTextColor: Int
        val cardBackground: Int
        if (isFocused) {
            titleTextColor = photonGrey10
            cardBackground = photonGrey60
        } else {
            titleTextColor = photonGrey10_a80p
            cardBackground = photonGrey70
        }

        with (holder) {
            titleView.setBackgroundColor(cardBackground)
            domainView.setBackgroundColor(cardBackground)

            titleView.setTextColor(titleTextColor)
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
    val domainView = itemView.findViewById<TextView>(R.id.domainView)
}
