/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.json.JSONException
import org.json.JSONObject

/**
 * Handles marshalling [PocketViewModel.FeedItem.Video] objects from JSON.
 */
object PocketVideoParser {

    fun parse(jsonObj: JSONObject): PocketViewModel.FeedItem.Video? = try {
        PocketViewModel.FeedItem.Video(
            id = jsonObj.getInt("id"),
            title = jsonObj.getString("title"),
            // Note that the 'url' property of our domain object can be retrieved from
            // either of two JSON fields, and we make no distinction internally
            url = jsonObj.optString("tv_url", null) ?: jsonObj.getString("url"),
            thumbnailURL = jsonObj.getString("image_src"),
            popularitySortId = jsonObj.getInt("popularity_sort_id")
        )
    } catch (e: JSONException) {
        null
    }
}
