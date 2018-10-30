/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.pocket_video_mega_tile.view.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.PicassoWrapper
import org.mozilla.tv.firefox.utils.RoundCornerTransformation

/** A view that contains the Pocket logo and several thumbnails from Pocket videos. */
class PocketVideoMegaTile(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    init {
        // The layout of this view is dependent on both parent and child layout params. To ensure we
        // don't lose some of these important parent params in another file (e.g. overlay), we set
        // them in code.
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END

        LayoutInflater.from(context).inflate(R.layout.pocket_video_mega_tile, this, true)
        PocketDrawable.setImageDrawableAsPocketWordmark(pocketWordmarkView)
    }

    fun setContent(feedItems: List<PocketViewModel.FeedItem>) {
        listOf(thumbnail1View, thumbnail2View, thumbnail3View, thumbnail4View)
            .zip(feedItems)
            .forEach { (view, item) ->
                when (item) {
                    is PocketViewModel.FeedItem.Video -> showFetchedImage(view, item.thumbnailURL)
                    is PocketViewModel.FeedItem.Loading -> showLocalDrawable(view, item.thumbnailResource)
                }
            }
    }

    private fun showLocalDrawable(view: ImageView, drawable: Int) {
        view.setImageResource(drawable)
    }

    private fun showFetchedImage(imageView: ImageView, url: String) {
        val roundCornerTransformation =
            RoundCornerTransformation(resources.getDimension(R.dimen.pocket_video_mega_tile_thumbnail_corner_radius))

        PicassoWrapper.client.load(url)
            .placeholder(R.drawable.pocket_placeholder)
            .transform(roundCornerTransformation)
            .into(imageView)
    }
}
