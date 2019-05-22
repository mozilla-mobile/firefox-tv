/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.AnyThread
import org.mozilla.tv.firefox.telemetry.SentryIntegration

private const val LOGTAG = "PocketVideoStore"

private const val KEY_VIDEO_JSON = "video_json"
private const val VIDEO_STORE_NAME = "Pocket-Global-Video-Recs"

private const val BUNDLED_VIDEOS_PATH = "bundled/pocket_videos.json"

/**
 * The minimum number of valid videos we must receive from the server if we want to display them.
 * This number is set to the number of videos that appear on screen at the time of writing.
 */
private const val REQUIRED_POCKET_VIDEO_COUNT = 4

/**
 * Saves the Pocket video recommendations as a raw JSON String and loads them in data structures for the app.
 * Bad data should saved so bad data should never be returned.
 *
 * This class is thread safe.
 */
class PocketVideoStore(
    appContext: Context,
    private val assets: AssetManager,
    private val convertJSONToPocketVideos: (String) -> List<PocketViewModel.FeedItem>?
) {

    // We use SharedPrefs because it's simple, it handles concurrency (so we don't even need to think about
    // which threads we access this from), and the file sizes will be small.
    private val sharedPrefs = appContext.getSharedPreferences(VIDEO_STORE_NAME, 0)

    /**
     * @return true if the save was successful, false if the save was not successful because the JSON is invalid.
     */
    @AnyThread
    fun save(json: String): Boolean {
        if (!isJSONValid(json)) {
            return false
        }

        sharedPrefs.edit()
            .putString(KEY_VIDEO_JSON, json)
            .apply()

        return true
    }

    private fun isJSONValid(rawJSON: String): Boolean {
        // While we don't need the conversion result, this function already handles validation so we use
        // it to validate the videos.
        val convertedVideos = convertJSONToPocketVideos(rawJSON)
        return convertedVideos != null &&

            // Guarantee a minimum number of Pocket videos: e.g. if the server only returns one valid video,
            // we wouldn't want to overwrite what the user already has to show only one video.
            convertedVideos.size >= REQUIRED_POCKET_VIDEO_COUNT
    }

    /**
     * @return the list of loaded videos. This should never happen but in case of error, the empty list is returned.
     */
    @AnyThread
    fun load(): List<PocketViewModel.FeedItem> {
        fun loadBundledTiles(): String = assets.open(BUNDLED_VIDEOS_PATH).bufferedReader().use { it.readText() }

        val rawJSON =  sharedPrefs.getString(KEY_VIDEO_JSON, null) ?: loadBundledTiles()

        val convertedVideos = convertJSONToPocketVideos(rawJSON)
        if (convertedVideos == null) {
            // We don't expect the conversion to ever fail: we only save valid JSON and we fallback to the presumably
            // valid bundled content if we've never saved. We don't crash because it may cause an infinite crash loop
            // for users but we record the error to Sentry. Users will see an undefined state: presumably the Pocket
            // channel will be empty but who knows if focusing around it will work correctly.
            Log.e(LOGTAG, "Error converting JSON to Pocket video")
            SentryIntegration.capture(IllegalStateException("Error converting JSON to Pocket video"))
            return emptyList()
        }

        return convertedVideos
    }
}
