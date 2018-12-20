/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

/**
 * Wraps a [PocketVideoRepo] instance and selectively forwards its emissions.
 * When frozen, no updates will be received from the [PocketVideoRepo].
 *
 * This is used to allow the [PocketVideoRepo] to update its state in the
 * background, without letting the displayed videos change when the user can
 * see them. This should be unfrozen when Pocket videos are not visible to
 * the user, and frozen when they are.
 *
 * This method is used rather than using lifecycles (that is, having ViewModels
 * that update once on init and then stop listening) because no single lifecycle
 * reflected the requirements. Specifically, navigating from overlay to Pocket
 * screen should not allow updates, but navigating from overlay to browser
 * should. This lets us manually control when updates are allowed.
 */
open class PocketRepoCache(private val repo: PocketVideoRepo) {

    private val _feedState = MutableLiveData<PocketVideoRepo.FeedState>() // Mutable backer for feedState
    open val feedState: LiveData<PocketVideoRepo.FeedState> = _feedState

    // This should only be unfrozen when Pocket videos are not visible to the user.
    // See class kdoc
    var frozen = false

    fun setup() {
        repo.feedState.observeForever {
            val cachedValueIsBad = _feedState.value !is PocketVideoRepo.FeedState.LoadComplete

            if (!frozen || cachedValueIsBad) {
                _feedState.postValue(it)
            }
        }
    }
}
