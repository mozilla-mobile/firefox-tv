/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.net.Uri
import android.util.Log
import androidx.annotation.AnyThread
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.executeAndAwait
import org.mozilla.tv.firefox.ext.flatMapObj
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import java.io.IOException
import java.util.concurrent.TimeoutException

private const val LOGTAG = "PocketEndpoint"

/**
 * Make requests to the Pocket endpoint and returns internal objects.
 *
 * The methods of this class call the endpoint directly and does not cache results or rate limit,
 * outside of the network layer (e.g. with OkHttp).
 */
open class PocketEndpoint(
    private val endpointRaw: PocketEndpointRaw,
    private val isPocketEnabledByLocale: () -> Boolean
) {

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
}

/** Make requests to the Pocket endpoint and returns raw data: see [PocketEndpoint] for more. */
class PocketEndpointRaw(
    private val appVersion: String,
    private val pocketGlobalVideoEndpoint: Uri?
) {

    /** @return The global video recommendations as a raw JSON str or null on error. */
    @AnyThread // executeAndAwait hands off the request to the OkHttp dispatcher.
    suspend fun getGlobalVideoRecommendations(): String? {
        pocketGlobalVideoEndpoint ?: return null

        val req = Request.Builder()
            .url(pocketGlobalVideoEndpoint.toString())
            .header("User-Agent", "FirefoxTV-$appVersion-${BuildConfig.BUILD_TYPE}")
            .build()

        val res = try {
            OkHttpWrapper.client.newCall(req).executeAndAwait()
        } catch (e: IOException) {
            Log.w(LOGTAG, "getGlobalVideoRecommendations: network error")
            Log.w(LOGTAG, e)
            return null
        } catch (e: TimeoutException) {
            Log.w(LOGTAG, "getGlobalVideoRecommendations: timed out")
            Log.w(LOGTAG, e)
            return null
        }

        return if (res.isSuccessful) res.body()?.string() else null
    }
}
