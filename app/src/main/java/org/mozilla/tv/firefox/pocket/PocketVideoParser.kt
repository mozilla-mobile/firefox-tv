/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.tv.firefox.ext.flatMapObj

private const val LOGTAG = "PocketVideoParser"

/**
 * Handles marshalling [PocketViewModel.FeedItem.Video] objects from JSON.
 */
object PocketVideoParser {

    // Ideally, this functionality would be in a separate class but 1) we're short on time and 2) this
    // functionality should be handled by the a-c implementation in the long term.
    /** @return The videos or null on error; the list will never be empty. */
    fun convertVideosJSON(jsonStr: String): List<PocketViewModel.FeedItem.Video>? = try {
        val rawJSON = JSONObject(jsonStr)
        val videosJSON = rawJSON.getJSONArray("recommendations")
        val videos = videosJSON.flatMapObj { PocketViewModel.FeedItem.Video.fromJSONObject(it) }
        if (videos.isNotEmpty()) videos else null
    } catch (e: JSONException) {
        Log.w(LOGTAG, "convertVideosJSON: invalid JSON from Pocket server")
        Log.w(LOGTAG, e)
        null
    }

    fun parse(jsonObj: JSONObject): PocketViewModel.FeedItem.Video? = try {
        PocketViewModel.FeedItem.Video(
            id = jsonObj.getInt("id"),
            title = jsonObj.getString("title"),
            // Note that the 'url' property of our domain object can be retrieved from
            // either of two JSON fields, and we make no distinction internally
            url = jsonObj.optString("tv_url", null) ?: jsonObj.getString("url"),
            thumbnailURL = jsonObj.getString("image_src"),
            popularitySortId = jsonObj.getInt("popularity_sort_id"),
            authors = getAuthorName(jsonObj)
        )
    } catch (e: JSONException) {
        null
    }

    private fun getAuthorName(jsonObj: JSONObject): String {
        return try {
            val authors = mutableListOf<String?>()
            val authorsJSON = JSONObject(jsonObj.getString("authors"))
            // TODO: verify if multiple authors are possible and what the format of authors object is
            for (x in authorsJSON.keys()) {
                val authorObject = JSONObject(authorsJSON[x].toString())
                authors += authorObject.getString("name")
            }
            authors.joinToString()
        } catch (e: JSONException) {
            ""
        }
    }
}
