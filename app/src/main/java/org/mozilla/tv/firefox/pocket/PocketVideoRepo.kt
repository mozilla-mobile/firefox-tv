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
    private val pocketVideoStore: PocketVideoStore,
    private val isPocketEnabledByLocale: () -> Boolean,
    isPocketKeyValid: Boolean
) {

    sealed class FeedState {
        data class LoadComplete(val videos: List<PocketViewModel.FeedItem>) : FeedState()
        object NoAPIKey : FeedState()
        object Inactive : FeedState()
    }

    private val _feedState = BehaviorSubject.createDefault(if (!isPocketKeyValid) FeedState.NoAPIKey else FeedState.Inactive)
    open val feedState = _feedState.hide()
        .observeOn(AndroidSchedulers.mainThread())
        .distinctUntilChanged() // avoid churn because we may retrieve similar results in onStart.

    @UiThread // not sure if this annotation is necessary anymore.
    fun updatePocketFromStore() {
        // If we have no API key, this will always be the state: there is nothing to do. In theory, now that we
        // ship with content, we could remove the NoAPIKey UI state but then it'd be less obvious if we accidentally
        // shipped a release build without a key: we should make that decision separately from this PR.
        if (_feedState.value == FeedState.NoAPIKey) {
            return
        }

        if (!isPocketEnabledByLocale()) {
            _feedState.onNext(FeedState.Inactive)
            return
        }

        val videos = pocketVideoStore.load()
        _feedState.onNext(FeedState.LoadComplete(videos))
    }
}
