/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.net.Uri
import android.util.Log
import androidx.annotation.AnyThread
import okhttp3.Request
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.executeAndAwait
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import java.io.IOException
import java.util.concurrent.TimeoutException

private const val LOGTAG = "PocketEndpointRaw"

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
