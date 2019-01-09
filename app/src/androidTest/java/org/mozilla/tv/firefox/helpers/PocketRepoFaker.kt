/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.pocket.PocketEndpoint
import org.mozilla.tv.firefox.pocket.PocketFeedStateMachine
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.utils.BuildConfigDerivables

/**
 * Provides a fake [PocketVideoRepo] implementation for testing purposes.
 *
 * Any values pushed to [fakedPocketRepoState] will be immediately emitted.
 */
object PocketRepoFaker {

    private val pocketEndpoint = object : PocketEndpoint("VERSION", "www.mock.com".toUri()) {
        override suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
            return PocketViewModel.noKeyPlaceholders
        }
    }

    private val localeIsEnglish: () -> Boolean = { true }

    val fakedPocketRepoState = MutableLiveData<PocketVideoRepo.FeedState>()
    val fakedPocketRepo = object : PocketVideoRepo(
        pocketEndpoint,
        PocketFeedStateMachine(),
        localeIsEnglish,
        BuildConfigDerivables(localeIsEnglish)
    ) {
        override val feedState: LiveData<FeedState>
            get() = fakedPocketRepoState
    }
}
