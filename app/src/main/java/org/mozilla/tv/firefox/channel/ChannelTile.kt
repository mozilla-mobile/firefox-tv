/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channel

import android.content.Context
import android.widget.ImageView
import org.mozilla.tv.firefox.pinnedtile.BundledPinnedTile
import org.mozilla.tv.firefox.pinnedtile.CustomPinnedTile
import org.mozilla.tv.firefox.pinnedtile.PinnedTileScreenshotStore
import org.mozilla.tv.firefox.utils.PicassoWrapper
import java.io.File
import java.util.UUID

/**
 * TODO
 */
data class ChannelTile(
        val url: String,
        val title: String,
        val setImage: (ImageView) -> Unit
)

data class ChannelDetails(
        // todo; change names, too similar
        val title: CharSequence,
        val tiles: List<ChannelTile>
)




//TODO move this into its own file
class ScreenshotStoreWrapper(private val context: Context) {
    fun getFileForUUID(id: UUID): File {
        return PinnedTileScreenshotStore.getFileForUUID(context, id)
    }
}
