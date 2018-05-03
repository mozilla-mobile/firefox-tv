/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.net.Uri
import android.support.annotation.AnyThread
import android.util.Log
import okhttp3.Request
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.ext.executeAndAwait
import org.mozilla.focus.utils.OkHttpWrapper
import java.io.IOException

private const val LOGTAG = "PocketEndpoint"

private const val PARAM_API_KEY = "consumer_key"

private val GLOBAL_VIDEO_ENDPOINT = Uri.parse("https://getpocket.cdn.mozilla.net/v3/firefox/global-video-recs")
        .buildUpon()
        .appendQueryParameter(PARAM_API_KEY, BuildConfig.POCKET_KEY)
        .build()

/**
 * Make requests to the Pocket Endpoint and returns raw data.
 *
 * The methods of this class call the endpoint directly and does not cache results or rate limit,
 * outside of the network layer (e.g. with OkHttp).
 */
internal object PocketEndpoint {

    /**
     * @return The global video recommendations as a raw JSON str or null on error.
     */
    @AnyThread // executeAndAwait hands off the request to the OkHttp dispatcher.
    suspend fun getGlobalVideoRecommendations(): String? {
        val req = Request.Builder()
                .url(GLOBAL_VIDEO_ENDPOINT.toString())
                .build()

        val res = try {
            OkHttpWrapper.client.newCall(req).executeAndAwait()
        } catch (e: IOException) {
            Log.w(LOGTAG, "network error in getGlobalVideoRecommendations")
            return null
        }

        return if (res.isSuccessful) res.body()?.string() else null
    }
}
