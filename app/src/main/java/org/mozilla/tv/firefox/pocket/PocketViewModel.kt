/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.ViewModel
import android.support.annotation.VisibleForTesting
import org.json.JSONObject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.map

const val POCKET_VIDEO_COUNT = 20

/**
 * Provides data that maps 1:1 to Pocket view state.
 *
 * This view state is provided by transforming backing state (provided by the
 * [PocketVideoRepo]), stripping information not important to the view, and adding
 * information required by the view. This should be enough to render (i.e., the
 * view should not have to perform any transformations on this data).
 */
class PocketViewModel(
    private val pocketVideoRepo: PocketVideoRepo,
    private val localeIsEnglish: () -> Boolean,
    pocketRepoCache: PocketRepoCache
) : ViewModel() {

    sealed class State {
        object Error : State()
        data class Feed(val feed: List<FeedItem>) : State()
        object NotDisplayed : State()
    }

    sealed class FeedItem {
        data class Loading(val thumbnailResource: Int) : FeedItem()
        data class Video(
            val id: Int,
            val title: String,
            val url: String,
            val thumbnailURL: String,
            val popularitySortId: Int,
            val authors: String
        ) : FeedItem() {
            companion object {
                fun fromJSONObject(jsonObject: JSONObject) = PocketVideoParser.parse(jsonObject)
            }
        }
    }

    val state = pocketRepoCache.feedState
        .map { repoState ->
            when (repoState) {
                is PocketVideoRepo.FeedState.Loading -> State.Feed(loadingPlaceholders)
                is PocketVideoRepo.FeedState.LoadComplete -> State.Feed(repoState.videos)
                is PocketVideoRepo.FeedState.NoAPIKey -> State.Feed(noKeyPlaceholders)
                is PocketVideoRepo.FeedState.FetchFailed -> State.Error
            }
        }.map {
            if (localeIsEnglish()) it
            // We only show the Pocket mega tile if the current language is English.
            // Otherwise, overwrite any state with NotDisplayed
            // See issue: https://github.com/mozilla-mobile/firefox-tv/issues/1283
            else State.NotDisplayed
        }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val loadingPlaceholders: List<FeedItem> =
            List(POCKET_VIDEO_COUNT) { FeedItem.Loading(R.color.photonGrey50) }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val noKeyPlaceholders: List<FeedItem> = List(POCKET_VIDEO_COUNT) {
            FeedItem.Video(
                id = it,
                title = "Mozilla",
                url = "https://www.mozilla.org/en-US/",
                thumbnailURL = "https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png",
                popularitySortId = it,
                authors = "0:{'name':'Mozilla'}"
            )
        }
    }

    fun update() = pocketVideoRepo.update()
}
