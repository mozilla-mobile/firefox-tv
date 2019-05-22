/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.annotation.UiThread
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 */
open class PocketVideoRepo(
    private val pocketFeedStateMachine: PocketFeedStateMachine,
    private val pocketVideoStore: PocketVideoStore,
    initialState: FeedState
) {

    sealed class FeedState {
        data class LoadComplete(val videos: List<PocketViewModel.FeedItem>) : FeedState()
        object Loading : FeedState()
        object NoAPIKey : FeedState()
        object FetchFailed : FeedState()
        object Inactive : FeedState()
    }

    private val _feedState = BehaviorSubject.createDefault(initialState)
    open val feedState = _feedState.hide()
        .observeOn(AndroidSchedulers.mainThread())
        .distinctUntilChanged() // avoid churn because we may retrieve similar results in onStart.

    @UiThread // not sure if this is necessary anymore.
    fun updatePocketFromStore() {
        val videos = pocketVideoStore.load()
        _feedState.onNext(FeedState.LoadComplete(videos))
    }
}
