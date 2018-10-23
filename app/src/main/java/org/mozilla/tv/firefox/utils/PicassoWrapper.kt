/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import org.mozilla.tv.firefox.ext.withRoundedCorners

/**
 * A holder for the shared Picasso instance. All calls to Picasso should go through this class.
 */
object PicassoWrapper {
    @JvmStatic
    val client get() = Picasso.get()
}

/**
 * Rounds the corners of the given Bitmap. This method will not work correctly if the Bitmap
 * content is skewed or cropped.
 *
 * This method isn't efficient: it creates a copy of the bitmap you're trying to round the
 * corners of. A more efficient method would be to create a Drawable or ImageView that draws
 * the Bitmap without having to modify it, but we don't have one of those implementations yet.
 */
class RoundCornerTransformation(private val radiusPx: Float) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val roundedBitmap = source.withRoundedCorners(radiusPx)
        source.recycle()
        return roundedBitmap
    }

    override fun key(): String = "RoundCornerTransformation-$radiusPx"
}
