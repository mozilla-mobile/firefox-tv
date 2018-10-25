/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

sealed class PocketRepoState {
    data class LoadComplete(val videos: List<PocketFeedItem>) : PocketRepoState()
    object Loading : PocketRepoState()
    object NoKey : PocketRepoState()
    object FetchFailed : PocketRepoState()
}

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 */
open class PocketRepo(private val pocketEndpoint: PocketEndpoint) {

    private val mutableState = MutableLiveData<PocketRepoState>()
    open val state: LiveData<PocketRepoState> = mutableState

}
