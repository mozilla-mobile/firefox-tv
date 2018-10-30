/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.mozilla.tv.firefox.pocket.PocketRepoState.FetchFailed
import org.mozilla.tv.firefox.pocket.PocketRepoState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketRepoState.Loading

/**
 * Manages all logic for updating cached [PocketRepoState] in a testable way.
 *
 * TODO
 */
class PocketRepoStateMachine {
    fun fromFetch(videos: List<PocketFeedItem>?, currentState: PocketRepoState): PocketRepoState {
        return when {
            videos?.isNotEmpty() == true -> LoadComplete(videos)
            currentState === Loading -> FetchFailed
            else -> currentState
        }
    }

    fun setLoading(currentState: PocketRepoState): PocketRepoState {
        return when {
            currentState === FetchFailed -> Loading
            else -> currentState
        }
    }
}
