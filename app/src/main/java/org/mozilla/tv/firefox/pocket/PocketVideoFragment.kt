/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_pocket_video.pocketHelpButton
import kotlinx.android.synthetic.main.fragment_pocket_video.pocketWordmarkView
import kotlinx.android.synthetic.main.fragment_pocket_video.videoFeed
import mozilla.components.support.ktx.android.os.resetAfter
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.architecture.FocusOnShowDelegate
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.updateLayoutParams
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.FormattedDomain
import org.mozilla.tv.firefox.utils.PicassoWrapper
import java.net.URI
import java.net.URISyntaxException

/** A feed of Pocket videos. */
class PocketVideoFragment : Fragment() {
    companion object {
        const val FRAGMENT_TAG = "pocket"
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_pocket_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pocketHelpButton.setOnClickListener { _ ->
            startActivity(Intent(context, PocketOnboardingActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val adapter = PocketVideoAdapter(context!!, fragmentManager!!).also {
            videoFeed.gridView.adapter = it
        }

        // SVGs can have artifacts if we set them in XML so we set it in code.
        PocketDrawable.setImageDrawableAsPocketWordmark(pocketWordmarkView)
        pocketHelpButton.setImageDrawable(context!!.getDrawable(R.drawable.pocket_onboarding_help_button))

        val viewModel = FirefoxViewModelProviders.of(this).get(PocketViewModel::class.java)

        viewModel.state
            .subscribe { state ->
                when (state) {
                    is PocketViewModel.State.Error -> { /* TODO: #769: display error screen */ }
                    is PocketViewModel.State.Feed -> adapter.setVideos(state.feed)
                    is PocketViewModel.State.NotDisplayed -> Unit // We may come here on non-EN locales
                    null -> { }
                }.forceExhaustive
            }
            .addTo(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        FocusOnShowDelegate().onHiddenChanged(this, hidden)
        super.onHiddenChanged(hidden)
    }
}

private class PocketVideoAdapter(
    context: Context,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<PocketVideoViewHolder>() {
    private val pocketVideos = mutableListOf<PocketViewModel.FeedItem>()

    private val photonGrey70 = ContextCompat.getColor(context, R.color.photonGrey70)
    private val photonGrey60 = ContextCompat.getColor(context, R.color.photonGrey60)
    private val photonGrey10 = ContextCompat.getColor(context, R.color.photonGrey10)
    private val photonGrey10_a80p = ContextCompat.getColor(context, R.color.photonGrey10_a80p)

    private val videoItemHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_video_item_horizontal_margin)
    private val feedHorizontalMargin = context.resources.getDimensionPixelSize(R.dimen.pocket_feed_horizontal_margin)

    fun setVideos(videos: List<PocketViewModel.FeedItem>) {
        pocketVideos.clear()
        pocketVideos.addAll(videos)
        notifyDataSetChanged()
    }

    override fun getItemCount() = pocketVideos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PocketVideoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.pocket_video_item, parent, false))

    override fun onBindViewHolder(holder: PocketVideoViewHolder, position: Int) = with(holder) {
        val item = pocketVideos[position]
        setHorizontalMargins(holder, position)

        when (item) {
            is PocketViewModel.FeedItem.Loading -> holder.bindPlaceholder()
            is PocketViewModel.FeedItem.Video -> holder.bindPocketVideo(item)
        }.forceExhaustive
    }

    private fun PocketVideoViewHolder.bindPlaceholder() {
        itemView.setOnClickListener(null)
        itemView.onFocusChangeListener = null
        titleView.text = ""
        domainView.text = ""
        videoThumbnailView.setImageResource(R.color.photonGrey50)
    }

    private fun PocketVideoViewHolder.bindPocketVideo(item: PocketViewModel.FeedItem.Video) {
        itemView.setOnClickListener {
            itemView.context!!.serviceLocator.screenController
                .showBrowserScreenForUrl(fragmentManager, item.url)
            TelemetryIntegration.INSTANCE.pocketVideoClickEvent(item.id)
        }
        itemView.setOnFocusChangeListener { _, hasFocus ->
            updateForFocusState(this, hasFocus)
            TelemetryIntegration.INSTANCE.pocketVideoImpressionEvent(item.id)
        }
        updateForFocusState(this, itemView.isFocused)

        titleView.text = item.title
        PicassoWrapper.client.load(item.thumbnailURL).into(videoThumbnailView)

        // TODO: Move this transformation into the VM (issue #1484)
        if (item.authors != "") domainView.text = item.authors
        else {
            @Suppress("TooGenericExceptionCaught") // See below.
            val itemURI = try {
                URI(item.url)
            } catch (e: Exception) { // Apparently Kotlin doesn't have multi-catch.
                when (e) {
                    is URISyntaxException, is NullPointerException -> null
                    else -> throw e
                }
            }
            domainView.text = if (itemURI == null) {
                item.url
            } else {
                // The first time this method is called ever, it may block until the file is cached on disk.
                // We pre-cache on startup so I'm hoping this isn't an issue.
                StrictMode.allowThreadDiskReads().resetAfter {
                    FormattedDomain.format(itemView.context, itemURI, false, 0)
                }
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

        with(holder) {
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
            marginEnd = if (position == pocketVideos.size - 1) feedHorizontalMargin else videoItemHorizontalMargin
        }
    }
}

private class PocketVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val videoThumbnailView = itemView.findViewById<ImageView>(R.id.videoThumbnailView)
    val titleView = itemView.findViewById<TextView>(R.id.titleView)
    val domainView = itemView.findViewById<TextView>(R.id.domainView)
}
