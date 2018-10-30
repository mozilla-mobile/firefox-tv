/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.ViewModel
import android.support.annotation.VisibleForTesting
import org.json.JSONObject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.pocket.PocketViewModelState.Error
import org.mozilla.tv.firefox.pocket.PocketViewModelState.Feed

const val POCKET_VIDEO_COUNT = 20

sealed class PocketViewModelState {
    object Error : PocketViewModelState()
    data class Feed(val feed: List<PocketFeedItem>) : PocketViewModelState()
}

sealed class PocketFeedItem {
    data class Loading(val thumbnailResource: Int) : PocketFeedItem()
    data class Video(
        val id: Int,
        val title: String,
        val url: String,
        val thumbnailURL: String,
        val popularitySortId: Int
    ) : PocketFeedItem() {
        companion object {
            fun fromJSONObject(jsonObject: JSONObject) = PocketVideoParser.parse(jsonObject)
        }
    }
}

/**
 * Provides data that maps 1:1 to Pocket view state.
 *
 * This view state is provided by transforming backing state (provided by the
 * [PocketRepo]), stripping information not important to the view, and adding
 * information required by the view. This should be enough to render (i.e., the
 * view should not have to perform any transformations on this data).
 */
class PocketViewModel(private val pocketRepo: PocketRepo, pocketRepoCache: PocketRepoCache) : ViewModel() {

    val state = pocketRepoCache.state.map { repoState ->
        when (repoState) {
            is PocketRepoState.Loading -> Feed(loadingPlaceholders)
            is PocketRepoState.LoadComplete -> Feed(repoState.videos)
            is PocketRepoState.NoKey -> Feed(noKeyPlaceholders)
            is PocketRepoState.FetchFailed -> Error
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val loadingPlaceholders: List<PocketFeedItem> =
        List(POCKET_VIDEO_COUNT) { PocketFeedItem.Loading(R.color.photonGrey50) }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val noKeyPlaceholders: List<PocketFeedItem> = List(POCKET_VIDEO_COUNT) {
        PocketFeedItem.Video(
            id = it,
            title = "Mozilla",
            url = "https://www.mozilla.org/en-US/",
            thumbnailURL = "https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png",
            popularitySortId = it
        )
    }

    fun update() = pocketRepo.update()
}
