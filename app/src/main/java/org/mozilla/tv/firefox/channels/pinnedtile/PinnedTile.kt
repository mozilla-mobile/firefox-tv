/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.pinnedtile

import androidx.annotation.WorkerThread
import org.json.JSONObject
import org.mozilla.tv.firefox.channels.ChannelTile
import org.mozilla.tv.firefox.channels.ImageSetStrategy
import org.mozilla.tv.firefox.ext.toJavaURI
import org.mozilla.tv.firefox.channels.TileSource
import org.mozilla.tv.firefox.utils.FormattedDomain
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import java.util.UUID

private const val KEY_URL = "url"
private const val KEY_TITLE = "title"

private const val KEY_IMG = "img"
private const val KEY_ID = "id"

sealed class PinnedTile(val url: String, val title: String) {
    fun idToString() = when (this) {
        is BundledPinnedTile -> id
        is CustomPinnedTile -> id.toString()
    }

    protected open fun toJSONObject() = JSONObject().apply {
        put(KEY_URL, url)
        put(KEY_TITLE, title)
    }

    @WorkerThread // This performs file access
    abstract fun toChannelTile(
        imageUtilityWrapper: PinnedTileImageUtilWrapper,
        formattedDomainWrapper: FormattedDomainWrapper
    ): ChannelTile
}

class BundledPinnedTile(
    url: String,
    title: String,
    val imagePath: String,
    /** Unique id used to identify specific home tiles, e.g. for deletion, etc. **/
    val id: String
) : PinnedTile(url, title) {

    public override fun toJSONObject() = super.toJSONObject().apply {
        put(KEY_IMG, imagePath)
        put(KEY_ID, id)
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): BundledPinnedTile {
            return BundledPinnedTile(jsonObject.getString(KEY_URL),
                    jsonObject.getString(KEY_TITLE),
                    jsonObject.getString(KEY_IMG),
                    jsonObject.getString(KEY_ID))
        }
    }

    override fun toChannelTile(
        imageUtilityWrapper: PinnedTileImageUtilWrapper,
        formattedDomainWrapper: FormattedDomainWrapper
    ): ChannelTile {
        return ChannelTile(
                url = url,
                title = title,
                subtitle = null,
                // TODO find a less brittle way to retrieve this path
                setImage = ImageSetStrategy.ByPath("file:///android_asset/bundled/$imagePath"),
                tileSource = TileSource.BUNDLED,
                id = idToString()
        )
    }
}

/**
 * @param title this is always "custom", and I have no idea why. We convert to a real value
 * in [toChannelTile]
 */
class CustomPinnedTile(
    url: String,
    title: String,
    /** Used by [PinnedTileScreenshotStore] to uniquely identify tiles. */
    val id: UUID
) : PinnedTile(url, title) {

    public override fun toJSONObject() = super.toJSONObject().apply {
        put(KEY_ID, id.toString())
    }

    override fun toChannelTile(
        imageUtilityWrapper: PinnedTileImageUtilWrapper,
        formattedDomainWrapper: FormattedDomainWrapper
    ): ChannelTile {
        val backup = imageUtilityWrapper.generatePinnedTilePlaceholder(url)

        return ChannelTile(
                url = url,
                title = createTitle(formattedDomainWrapper),
                subtitle = null,
                setImage = ImageSetStrategy.ByFile(imageUtilityWrapper.getFileForUUID(id), backup),
                // todo: fix scope, double check this tileSource is okay.
                tileSource = TileSource.CUSTOM,
                id = idToString()
        )
    }

    // CustomPinnedTile titles are not accurate. See class kdoc
    private fun createTitle(formattedDomainWrapper: FormattedDomainWrapper): String {
        val validUri = url.toJavaURI() ?: return url

        val subdomainDotDomain = formattedDomainWrapper.format(validUri, false, 1)
        return FormattedDomain.stripCommonPrefixes(subdomainDotDomain)
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject) = CustomPinnedTile(
                url = jsonObject.getString(KEY_URL),
                title = jsonObject.getString(KEY_TITLE),
                id = UUID.fromString(jsonObject.getString(KEY_ID))
        )
    }
}
