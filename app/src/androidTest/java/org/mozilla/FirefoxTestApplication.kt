/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.pocket.PocketEndpoint
import org.mozilla.tv.firefox.pocket.PocketFeedStateMachine
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import org.mozilla.tv.firefox.utils.ServiceLocator

@Suppress("UNCHECKED_CAST")
class FirefoxTestApplication : FirefoxApplication() {

    private val pocketEndpoint = object : PocketEndpoint() {
        override suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
            return PocketViewModel.noKeyPlaceholders as List<PocketViewModel.FeedItem.Video>
        }
    }

    private val pocketVideoRepoState = MutableLiveData<PocketVideoRepo.FeedState>()

    private val pocketVideoRepo = object : PocketVideoRepo(pocketEndpoint, PocketFeedStateMachine(), BuildConfigDerivables()) {
        override val feedState: LiveData<FeedState>
            get() = pocketVideoRepoState
    }

    override fun createServiceLocator() = object : ServiceLocator(this) {
        override val pocketRepo = pocketVideoRepo
    }

    fun pushPocketRepoState(state: PocketVideoRepo.FeedState) = pocketVideoRepoState.postValue(state)
}
