/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import mozilla.components.support.ktx.android.graphics.withRoundedCorners
import org.mozilla.tv.firefox.R
import java.io.File
import java.util.UUID

/**
 * TODO
 * lets us set up context then put this in the service locator.  Allows us to keep context out of VMs
 */
class PinnedTileImageUtilWrapper(private val context: Context) {
    fun getFileForUUID(id: UUID): File {
        return PinnedTileScreenshotStore.getFileForUUID(context, id)
    }

    fun generatePinnedTilePlaceholder(url: String): Drawable {
        val homeTilePlaceholderCornerRadius = context.resources.getDimension(R.dimen.home_tile_placeholder_corner_radius)

        return PinnedTilePlaceholderGenerator.generate(context, url)
                .withRoundedCorners(homeTilePlaceholderCornerRadius)
                .toDrawable(context.resources)
    }
}
