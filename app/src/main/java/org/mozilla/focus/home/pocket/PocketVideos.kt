/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import org.json.JSONException
import org.json.JSONObject

/**
 * TODO name conflict with PocketVideo.
 */
object PocketVideos {
}

data class PocketVideo(
        val title: String,
        val url: String,
        val dedupeURL: String,
        val thumbnailURL: String
) {

    companion object {
        fun fromJSONObject(jsonObj: JSONObject) = try {
            PocketVideo(
                    title = jsonObj.getString("title"),
                    url = jsonObj.getString("url"),
                    dedupeURL = jsonObj.getString("dedupe_url"),
                    thumbnailURL = jsonObj.getString("image_src")
            )
        } catch (e: JSONException) {
            null
        }
    }
}
