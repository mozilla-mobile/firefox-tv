/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import org.json.JSONObject
import org.mozilla.tv.firefox.channel.ChannelTile
import org.mozilla.tv.firefox.utils.PicassoWrapper
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



    // TODO wrap PinnedTileScreenshotStore in a wrapper that has context, attach it to ServiceLocator.  Then we won't need context inside the VM
    abstract fun toChannelTile(imageUtilityWrapper: PinnedTileImageUtilWrapper): ChannelTile
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

    override fun toChannelTile(imageUtilityWrapper: PinnedTileImageUtilWrapper): ChannelTile {
        return ChannelTile(
                url = url,
                title = title,
                setImage = { view -> PicassoWrapper.client
                        .load("file:///android_asset/bundled/$imagePath") //TODO do this better
                        .into(view) }
        )
    }
}

class CustomPinnedTile(
    url: String,
    title: String, // TODO: this title is always "custom". We have some relatively complex logic in PinnedTileAdapter#onBindCustomHomeTile to get the real title
    /** Used by [PinnedTileScreenshotStore] to uniquely identify tiles. */
    val id: UUID
) : PinnedTile(url, title) {

    public override fun toJSONObject() = super.toJSONObject().apply {
        put(KEY_ID, id.toString())
    }

    override fun toChannelTile(imageUtilityWrapper: PinnedTileImageUtilWrapper): ChannelTile {
        val backup = imageUtilityWrapper.generatePinnedTilePlaceholder(url)

        return ChannelTile(
                url = url,
                title = title,
                setImage = { view -> PicassoWrapper.client
                        .load(imageUtilityWrapper.getFileForUUID(id))
                        .placeholder(backup)
                        .into(view) } // todo: fix scope, double check this is okay.
        )
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject) = CustomPinnedTile(
                url = jsonObject.getString(KEY_URL),
                title = jsonObject.getString(KEY_TITLE),
                id = UUID.fromString(jsonObject.getString(KEY_ID))
        )
    }
}
