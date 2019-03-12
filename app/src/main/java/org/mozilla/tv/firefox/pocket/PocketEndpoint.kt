/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.VisibleForTesting
import android.util.Log
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.executeAndAwait
import org.mozilla.tv.firefox.ext.flatMapObj
import org.mozilla.tv.firefox.utils.Endpoint
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import org.mozilla.tv.firefox.utils.Response
import java.io.IOException

private const val LOGTAG = "PocketEndpoint"

typealias PocketData = List<PocketViewModel.FeedItem.Video>

/**
 * Make requests to the Pocket endpoint and returns internal objects.
 *
 * The methods of this class call the endpoint directly and does not cache results or rate limit,
 * outside of the network layer (e.g. with OkHttp).
 */
open class PocketEndpoint(
    private val appVersion: String,
    private val pocketEndpoint: Uri?,
    private val getIsEnglishLocale: () -> Boolean
) : Endpoint<PocketData> {

    /**
     * Wraps the suspend function [getRecommendedVideos] in a [Single]
     *
     * Note that this is a blocking call
     */
    override fun request(): Single<Response<PocketData>> {
        val videos = try {
            runBlocking { getRecommendedVideos() }
        } catch (e: InterruptedException) {
            // RxJava disposals briefly interrupt their threads, which here will
            // cause runBlocking to crash. We can treat this as a failed response
            // without any additional handling.
            null
        }
        return when {
            videos == null || videos.isEmpty() -> Single.just(Response.Failure())
            else -> Single.just(Response.Success(videos))
        }
    }

    /** @return The global video recommendations or null on error; the list will never be empty. */
    @AnyThread // via PocketEndpointRaw.
    open suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
        if (!getIsEnglishLocale.invoke()) return null
        pocketEndpoint ?: return null
        val jsonResponse = PocketEndpointRaw.getGlobalVideoRecommendations(appVersion, pocketEndpoint) ?: return null
        return convertVideosJSON(jsonResponse)
    }

    /** @return The videos or null on error; the list will never be empty. */
    @VisibleForTesting fun convertVideosJSON(jsonStr: String): List<PocketViewModel.FeedItem.Video>? = try {
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
private object PocketEndpointRaw {

    /** @return The global video recommendations as a raw JSON str or null on error. */
    @AnyThread // executeAndAwait hands off the request to the OkHttp dispatcher.
    suspend fun getGlobalVideoRecommendations(version: String, pocketEndpoint: Uri): String? {
        val req = Request.Builder()
            .url(pocketEndpoint.toString())
            .header("User-Agent", "FirefoxTV-$version-${BuildConfig.BUILD_TYPE}")
            .build()

        val res = try {
            OkHttpWrapper.client.newCall(req).executeAndAwait()
        } catch (e: IOException) {
            Log.w(LOGTAG, "getGlobalVideoRecommendations: network error")
            Log.w(LOGTAG, e)
            return null
        }

        return if (res.isSuccessful) res.body()?.string() else null
    }
}
