/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.net.Uri
import android.support.annotation.AnyThread
import android.support.annotation.VisibleForTesting
import android.util.Log
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.executeAndAwait
import org.mozilla.tv.firefox.ext.flatMapObj
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import java.io.IOException

private const val LOGTAG = "PocketEndpoint"

private const val PARAM_API_KEY = "consumer_key"

private val GLOBAL_VIDEO_ENDPOINT = Uri.parse("https://getpocket.cdn.mozilla.net/v3/firefox/global-video-recs")
        .buildUpon()
        .appendQueryParameter(PARAM_API_KEY, BuildConfig.POCKET_KEY)
        .appendQueryParameter("version", "2")
        .build()

/**
 * Make requests to the Pocket endpoint and returns internal objects.
 *
 * The methods of this class call the endpoint directly and does not cache results or rate limit,
 * outside of the network layer (e.g. with OkHttp).
 */
object PocketEndpoint {

    /** @return The global video recommendations or null on error; the list will never be empty. */
    @AnyThread // via PocketEndpointRaw.
    suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
        val videosJSON = PocketEndpointRaw.getGlobalVideoRecommendations() ?: return null
        return convertVideosJSON(videosJSON)
    }

    /** @return The videos or null on error; the list will never be empty. */
    @VisibleForTesting fun convertVideosJSON(jsonStr: String): List<PocketViewModel.FeedItem.Video>? = try {
        val rawJSON = JSONObject(jsonStr)
        val videosJSON = rawJSON.getJSONArray("recommendations")
        val videos = videosJSON.flatMapObj { PocketViewModel.FeedItem.Video.fromJSONObject(it) }
        if (videos.isNotEmpty()) videos else null
    } catch (e: JSONException) {
        Log.w(LOGTAG, "convertVideosJSON: invalid JSON from Pocket server")
        null
    }
}

/** Make requests to the Pocket endpoint and returns raw data: see [PocketEndpoint] for more. */
private object PocketEndpointRaw {

    /** @return The global video recommendations as a raw JSON str or null on error. */
    @AnyThread // executeAndAwait hands off the request to the OkHttp dispatcher.
    suspend fun getGlobalVideoRecommendations(): String? {
        val req = Request.Builder()
                .url(GLOBAL_VIDEO_ENDPOINT.toString())
                .build()

        val res = try {
            OkHttpWrapper.client.newCall(req).executeAndAwait()
        } catch (e: IOException) {
            Log.w(LOGTAG, "getGlobalVideoRecommendations: network error")
            return null
        }

        return if (res.isSuccessful) res.body()?.string() else null
    }
}
