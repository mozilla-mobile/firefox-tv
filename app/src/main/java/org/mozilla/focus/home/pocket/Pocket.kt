/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home.pocket

import android.os.SystemClock
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import java.util.concurrent.TimeUnit

/**
 * Time between updates to the cache. The video feed is updated once an hour and the network
 * API says we should cache the data for 30 minutes: for simplicity, we hardcode instead of getting
 * it from the server and we split the difference so updates aren't too frequent.
 */
private val CACHE_UPDATE_FREQUENCY_MILLIS = TimeUnit.MINUTES.toMillis(45)

typealias PocketVideosDeferred = Deferred<List<PocketVideo>?>

/**
 * A public accessor for getting the Pocket data. This class has the following responsibilities:
 * - Public API access point
 * - Cache container
 * - Cache update scheduler & updater
 *
 * We cache in memory only. We chose not to cache on disk because it adds complexity and the cache
 * is likely to be severely outdated between runs of the application anyway.
 */
object Pocket {

    @Volatile private lateinit var videosCache: PocketVideosDeferred
    @Volatile private var lastUpdateMillis = -1L
    private val nextUpdateMillis get() = lastUpdateMillis + CACHE_UPDATE_FREQUENCY_MILLIS

    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread
    @set:UiThread
    private var backgroundUpdates: Job? = null

    fun init() {
        // We set this now, rather than waiting for the background updates, to ensure the first
        // caller gets a Deferred they can wait on, rather than null (which they can't wait on).
        @Suppress("SENSELESS_COMPARISON") // Values of BuildConfig can change but the compiler doesn't know that..
        videosCache = if (BuildConfig.POCKET_KEY != null) getRecommendedVideosNoCacheAsync()
        else getPlaceholderVideos()
        lastUpdateMillis = SystemClock.elapsedRealtime()
    }

    @UiThread // update backgroundUpdates.
    fun startBackgroundUpdates() {
        backgroundUpdates?.cancel(CancellationException("Cancelling unexpectedly active job to ensure only one is running"))
        backgroundUpdates = startBackgroundUpdatesInner()
    }

    @UiThread // update backgroundUpdates.
    fun stopBackgroundUpdates() {
        backgroundUpdates?.cancel(CancellationException("Stopping background updates"))
        backgroundUpdates = null
    }

    /**
     * Gets the recommended Pocket videos from cache. The returned [Deferred] will always contain a
     * value except on start-up, when the initial request may still be in process. The returned value
     * may be null which occurs when there's been an error setting an initial value in the feed.
     *
     * The videos are updated asynchronously so calling this twice in succession may return
     * different lists.
     *
     * This method assumes [startBackgroundUpdates] has been called before being accessed.
     */
    @AnyThread // videosCache is synchronized.
    fun getRecommendedVideos() = videosCache
    private fun getRecommendedVideosNoCacheAsync() = async { PocketEndpoint.getRecommendedVideos() }
    private fun getPlaceholderVideos() = async { PocketEndpoint.getPlaceholderVideos() }

    private fun startBackgroundUpdatesInner() = launch {
        while (true) {
            val delayForMillis = nextUpdateMillis - SystemClock.elapsedRealtime()
            if (delayForMillis > 0) {
                delay(delayForMillis, TimeUnit.MILLISECONDS)
            }

            val deferredVideoUpdate = getRecommendedVideosNoCacheAsync()

            // We only want to update the cache 1) after the request completes so the user never has
            // to see a loading screen except for the initial load and 2) if the request has been
            // successful (i.e. non-null) so the user never has to see an error screen (unless we
            // never got data initially).
            val videoUpdate = deferredVideoUpdate.await()
            if (videoUpdate != null) {
                videosCache = deferredVideoUpdate
                lastUpdateMillis = SystemClock.elapsedRealtime()
            }
        }
    }
}

sealed class PocketFeedItem
object PocketPlaceholder : PocketFeedItem()
data class PocketVideo(
    val id: Int,
    val title: String,
    val url: String,
    val thumbnailURL: String,
    val popularitySortId: Int
) : PocketFeedItem() {

    companion object {
        fun fromJSONObject(jsonObj: JSONObject): PocketVideo? = try {
            PocketVideo(
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
}

fun ImageView.setImageDrawableAsPocketWordmark() {
    // We want to set SVGs in code because they can produce artifacts otherwise.
    setImageDrawable(context.getDrawable(R.drawable.ic_pocket_and_wordmark).apply {
        setTint(ContextCompat.getColor(context, R.color.photonGrey10))
    })
}
