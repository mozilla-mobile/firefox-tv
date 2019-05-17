/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.pinnedtile

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import mozilla.components.support.ktx.android.graphics.withRoundedCorners
import org.mozilla.tv.firefox.R
import java.io.File
import java.util.UUID

/**
 * Wraps an instance of [Application]. We then store this class put this in the service locator,
 * allowing us to keep context out of ViewModels
 */
class PinnedTileImageUtilWrapper(private val application: Application) {
    fun getFileForUUID(id: UUID): File {
        return PinnedTileScreenshotStore.getFileForUUID(application, id)
    }

    fun generatePinnedTilePlaceholder(url: String): Drawable {
        val homeTilePlaceholderCornerRadius = application.resources.getDimension(R.dimen.home_tile_placeholder_corner_radius)

        return PinnedTilePlaceholderGenerator.generate(application, url)
                .withRoundedCorners(homeTilePlaceholderCornerRadius)
                .toDrawable(application.resources)
    }
}
