/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket


import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.Loading
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.NoKey
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.FetchFailed

/**
 * Manages all logic for updating cached [PocketVideoRepo.FeedState] in a testable way.
 *
 * ### Valid State Machine Transitions (any other transitions will return the current feed state)
 * - Any -> [LoadComplete]
 * - [LoadComplete] -> new [LoadComplete]
 * - [FetchFailed] -> [Loading]
 * - [Loading] -> [FetchFailed]
 *
 *
 * ### Diagram
 * [Loading] ------------------
 *    |  ^                    |     ---
 *    V  |                    V     | V
 * [FetchFailed] ---------> [LoadComplete]
 *                            ^
 *                            |
 * [NoKey]   ------------------
 *
 * ### Valid Starting Configurations
 * - [Loading]
 * - [NoKey]
 */
class PocketFeedStateMachine {
    fun computeNewState(
        repoState: PocketVideoRepo.FeedState,
        cacheState: PocketVideoRepo.FeedState?
    ): PocketVideoRepo.FeedState {
        return when {
            repoState is LoadComplete -> return repoState
            repoState === Loading && cacheState === FetchFailed -> return repoState
            repoState === FetchFailed && cacheState === Loading -> return repoState
            cacheState == null -> repoState
            else -> cacheState
        }
    }
}
