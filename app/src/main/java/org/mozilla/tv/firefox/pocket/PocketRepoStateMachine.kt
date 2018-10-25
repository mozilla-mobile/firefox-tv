/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.mozilla.tv.firefox.pocket.PocketRepoState.FetchFailed
import org.mozilla.tv.firefox.pocket.PocketRepoState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketRepoState.Loading
import org.mozilla.tv.firefox.pocket.PocketRepoState.NoKey

/**
 * Manages all logic for updating cached [PocketRepoState] in a testable way.
 *
 * When instantiated with cached state and a new value, [computedState] will return
 * whatever the new state should be.
 */
class PocketRepoStateMachine(private val inputState: PocketRepoState, private val cachedState: PocketRepoState) {

    fun computedState(): PocketRepoState {
        return when (inputState) {
            is LoadComplete -> fromLoadCompleteInput(inputState)
            is NoKey -> fromNoKeyInput(inputState)
            is Loading -> fromLoadingInput()
            is FetchFailed -> fromFetchFailedInput()
        }
    }

    private fun fromLoadCompleteInput(loadComplete: LoadComplete): LoadComplete {
        return loadComplete
    }

    private fun fromNoKeyInput(noKey: NoKey): NoKey {
        return noKey
    }

    private fun fromLoadingInput(): PocketRepoState {
        return when {
            cachedState === FetchFailed -> Loading
            else -> cachedState
        }
    }

    private fun fromFetchFailedInput(): PocketRepoState {
        return when {
            cachedState === Loading -> FetchFailed
            else -> cachedState
        }
    }
}
