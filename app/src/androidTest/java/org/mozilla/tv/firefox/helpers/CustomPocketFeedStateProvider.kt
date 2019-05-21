/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import org.mozilla.tv.firefox.pocket.PocketEndpoint
import org.mozilla.tv.firefox.pocket.PocketEndpointRaw
import org.mozilla.tv.firefox.pocket.PocketFeedStateMachine
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.utils.BuildConfigDerivables

/**
 * Provides a fake [PocketVideoRepo] implementation for testing purposes.
 *
 * Any values pushed to [fakedPocketRepoState] will be immediately emitted.
 */
class CustomPocketFeedStateProvider {

    private val localeIsEnglish: () -> Boolean = { true }

    // Because of the endpoint overrides, the raw endpoint is unused so the arguments don't matter.
    private val pocketEndpointRaw = PocketEndpointRaw("001", null)
    private val pocketEndpoint = object : PocketEndpoint(pocketEndpointRaw, localeIsEnglish) {
        override suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
            return PocketViewModel.noKeyPlaceholders
        }
    }

    val fakedPocketRepoState = PublishSubject.create<PocketVideoRepo.FeedState>()
    val fakedPocketRepo = object : PocketVideoRepo(
        pocketEndpoint,
        PocketFeedStateMachine(),
        BuildConfigDerivables(localeIsEnglish).initialPocketRepoState
    ) {
        override val feedState: Observable<FeedState>
            get() = fakedPocketRepoState
                .observeOn(AndroidSchedulers.mainThread())
    }
}
