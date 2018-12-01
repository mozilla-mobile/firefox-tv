/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import org.json.JSONObject
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
}

class CustomPinnedTile(
    url: String,
    title: String,
    /** Used by [PinnedTileScreenshotStore] to uniquely identify tiles. */
    val id: UUID
) : PinnedTile(url, title) {

    public override fun toJSONObject() = super.toJSONObject().apply {
        put(KEY_ID, id.toString())
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject) = CustomPinnedTile(
                url = jsonObject.getString(KEY_URL),
                title = jsonObject.getString(KEY_TITLE),
                id = UUID.fromString(jsonObject.getString(KEY_ID))
        )
    }
}

fun PinnedTile.isEventTile(): Boolean =
    this.url == "http://www.mozilla.org/firefox/concerts?utm_source=firetv&utm_campaign" +
        "=livenation-promotion&utm_medium=referral&utm_content=firetv_tile"
