/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.pocket_video_mega_tile.view.*
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.utils.PicassoWrapper
import org.mozilla.focus.utils.RoundCornerTransformation
import kotlin.properties.Delegates

/** A view that contains the Pocket logo and several thumbnails from Pocket videos. */
class PocketVideoMegaTile(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    var pocketVideos by Delegates.observable<List<PocketVideo>?>(null) { _, _, newVideos ->
        // When no Pocket API key is provided, show placeholder tiles (developer ergonomics)
        if (BuildConfig.POCKET_KEY == null) {
            thumbnailViews.forEachIndexed { _, thumbnailView ->
                PicassoWrapper.client.load("https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png")
                        .transform(roundCornerTransformation)
                        .into(thumbnailView)
            }
            Toast.makeText(context, "Pocket API key was not found.", Toast.LENGTH_LONG).show()
        } else {
            if (newVideos == null) return@observable
            thumbnailViews.forEachIndexed { i, thumbnailView ->
                PicassoWrapper.client.load(newVideos[i].thumbnailURL)
                        .transform(roundCornerTransformation)
                        .into(thumbnailView)
            }
        }
    }

    private var thumbnailViews: List<ImageView>

    private val roundCornerTransformation = RoundCornerTransformation(
            resources.getDimension(R.dimen.pocket_video_mega_tile_thumbnail_corner_radius))

    init {
        // The layout of this view is dependent on both parent and child layout params. To ensure we
        // don't lose some of these important parent params in another file (e.g. overlay), we set
        // them in code.
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END

        LayoutInflater.from(context).inflate(R.layout.pocket_video_mega_tile, this, true)
        pocketWordmarkView.setImageDrawableAsPocketWordmark()
        thumbnailViews = listOf(thumbnail1View, thumbnail2View, thumbnail3View, thumbnail4View)
    }
}
