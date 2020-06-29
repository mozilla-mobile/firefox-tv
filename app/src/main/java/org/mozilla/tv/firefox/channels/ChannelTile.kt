/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Transformation
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.getDimenPixelSize
import org.mozilla.tv.firefox.utils.PicassoWrapper
import java.io.File

enum class TileSource { BUNDLED, CUSTOM, NEWS, SPORTS, MUSIC }

/**
 * Backing data for a [RecyclerView] item in a channel
 */
data class ChannelTile(
    val url: String,
    val title: String,
    val subtitle: String?,
    val setImage: ImageSetStrategy,
    // tileSource should not be used to change UI, which should be generic. It
    // is currently only used for telemetry
    val tileSource: TileSource,
    val id: String
) {
    /**
     * Generate localized modal title. Check against [tileSource] for channel type
     * See usage in [DefaultChannelAdapter.onBindViewHolder]
     */
    fun generateRemoveTileTitleStr(context: Context): String {
        return when (tileSource) {
            TileSource.BUNDLED, TileSource.CUSTOM ->
                context.resources.getString(R.string.pinned_tiles_channel_remove_title, title)
            TileSource.NEWS ->
                context.resources.getString(R.string.news_channel_remove_title, title)
            TileSource.SPORTS ->
                context.resources.getString(R.string.sports_channel_remove_title, title)
            TileSource.MUSIC ->
                context.resources.getString(R.string.music_channel_remove_title, title)
        }
    }

    companion object {
        /**
         * Set the horizontal margins on the given view.
         *
         * We want to add padding to the beginning and end of the RecyclerView: ideally we'd just add
         * paddingStart/End. Unfortunately, this causes a visual glitch as each card scrolls offscreen.
         * Instead, we set the margins for the first and last card.
         *
         * See [DefaultChannelAdapter.onBindViewHolder] for an example
         */
        fun setChannelMarginByPosition(view: View, context: Context, position: Int, itemCount: Int) {
            val defaultItemHorizontalMargin = context.getDimenPixelSize(R.dimen.pocket_video_item_horizontal_margin)
            val overlayMarginStart = context.getDimenPixelSize(R.dimen.overlay_margin_start)
            val overlayMarginEnd = context.getDimenPixelSize(R.dimen.overlay_margin_end)

            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // We need to reset margins on every view, not just first/last, because the View instance can be re-used.
                marginStart = if (position == 0) overlayMarginStart else defaultItemHorizontalMargin
                marginEnd = if (position == itemCount - 1) overlayMarginEnd else defaultItemHorizontalMargin
            }
        }
    }
}

/**
 * Subclasses of this define strategies for loading images, allowing [ChannelTile]s to solve this
 * problems in different ways.
 *
 * "Strategy" refers to the Go4 Strategy Pattern.
 *
 * Image setting was previously achieved using an [(ImageView) -> Unit], however these broke
 * equality assertions in unit tests (lambdas use referential equality).
 */
sealed class ImageSetStrategy {
    abstract operator fun invoke(imageView: ImageView)

    protected var transformation: Transformation? = null

    // This cannot be done during strategy instantiation because it will often require
    // information about the target ImageView (particularly its size)
    fun setTransformation(transformation: Transformation): ImageSetStrategy {
        this.transformation = transformation
        return this
    }

    protected fun RequestCreator.applyTransformationIfNotNull(transformation: Transformation?): RequestCreator {
        return if (transformation != null) this.transform(transformation)
        else this
    }

    data class ById(val id: Int) : ImageSetStrategy() {
        override fun invoke(imageView: ImageView) {
            // Picasso doesn't support SVGs, so we need to do a little extra work to be
            // able to apply our transformation
            // See https://github.com/square/picasso/issues/1109
            val bitmap = imageView.context.resources.getDrawable(id, null)
                .toBitmap()
                .let {
                    val transformation = transformation
                    if (transformation != null) transformation.transform(it)
                    else it
                }

            imageView.setImageBitmap(bitmap)
        }
    }

    // Note that ByPath can be used with either local paths or URLs
    data class ByPath(val path: String, val placeholderId: Int? = null, val errorId: Int? = null) : ImageSetStrategy() {
        override fun invoke(imageView: ImageView) {
            PicassoWrapper.client
                .load(path)
                .applyTransformationIfNotNull(transformation)
                .let { requestCreator ->
                    if (placeholderId != null) requestCreator.placeholder(placeholderId)
                    if (errorId == null) requestCreator
                    else requestCreator.error(errorId)
                }
                .into(imageView)
        }
    }

    data class ByFile(val file: File, val backup: Drawable) : ImageSetStrategy() {
        override fun invoke(imageView: ImageView) {
            PicassoWrapper.client
                .load(file)
                .placeholder(backup)
                .applyTransformationIfNotNull(transformation)
                .into(imageView)
        }
    }
}

/**
 * Backing data for a channel as a whole
 */
data class ChannelDetails(
    val title: CharSequence,
    val subtitle: CharSequence? = null,
    val tileList: List<ChannelTile>
)
