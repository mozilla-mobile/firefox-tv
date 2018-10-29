/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat.setTint
import android.widget.ImageView
import org.mozilla.tv.firefox.R

/**
 * Namespace for functions specific to setting Pocket drawables.
 */
object PocketDrawable {
    // This cannot be a private function because it is used in both the overlay and the Pocket screen
    fun setImageDrawableAsPocketWordmark(imageView: ImageView) {
        // We want to set SVGs in code because they can produce artifacts otherwise.
        imageView.setImageDrawable(imageView.context.getDrawable(R.drawable.ic_pocket_and_wordmark)!!.apply {
            setTint(ContextCompat.getColor(imageView.context, R.color.photonGrey10))
        })
    }
}
