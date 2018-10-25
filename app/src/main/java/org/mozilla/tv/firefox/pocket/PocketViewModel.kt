/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

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
    ) : PocketFeedItem()
}

/**
 * Provides data that maps 1:1 to Pocket view state.
 *
 * This view state is provided by transforming backing state (provided by the
 * [PocketRepo]), stripping information not important to the view, and adding
 * information required by the view. This should be enough to render (i.e., the
 * view should not have to perform any transformations on this data).
 */
class PocketViewModel(pocketRepo: PocketRepo) : ViewModel() {

    val state = MutableLiveData<PocketViewModelState>()

}
