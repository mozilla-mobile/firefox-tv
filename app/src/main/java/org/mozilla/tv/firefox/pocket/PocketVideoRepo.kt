/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.Companion.newInstance

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 *
 * To get an instance, use [newInstance] rather than the constructor.
 */
open class PocketVideoRepo @VisibleForTesting(otherwise = PRIVATE) constructor(
    private val pocketVideoStore: PocketVideoStore,
    private val isPocketEnabledByLocale: () -> Boolean,
    private val _feedState: BehaviorSubject<FeedState>
) {

    sealed class FeedState {
        data class LoadComplete(val videos: List<PocketViewModel.FeedItem>) : FeedState()
        object NoAPIKey : FeedState()
        object Inactive : FeedState()
    }

    open val feedState = _feedState.hide()
        .observeOn(AndroidSchedulers.mainThread())
        .distinctUntilChanged() // avoid churn because we may retrieve similar results in onStart.

    /**
     * Gets the latest Pocket videos from the store and pushes it to the UI. This is intentionally
     * not reactive because a reactive update model might replace the content when the user is looking
     * at it. This method is expected to be called when the user is not looking at the content (e.g. in onStart).
     */
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

    companion object {
        /**
         * Returns a new [PocketVideoRepo]: this is the preferred way to construct an instance.
         */
        fun newInstance(
            videoStore: PocketVideoStore,
            isPocketEnabledByLocale: () -> Boolean,
            isPocketKeyValid: Boolean
        ): PocketVideoRepo {
            val feedState = BehaviorSubject.createDefault(if (!isPocketKeyValid) FeedState.NoAPIKey else FeedState.Inactive)
            return PocketVideoRepo(videoStore, isPocketEnabledByLocale, feedState)
        }
    }
}
