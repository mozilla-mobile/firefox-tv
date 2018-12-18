/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.Loading
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.NoAPIKey
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.FetchFailed
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.Inactive

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
 *    |  ^                    |           ---
 *    V  |                    V           | V
 * [FetchFailed] ---------> [LoadComplete] / [Inactive]
 *                            ^
 *                            |
 * [NoAPIKey]   ---------------
 *
 * ### Valid Starting Configurations
 * - [Loading]
 * - [NoAPIKey]
 * - [Inactive]
 */
class PocketFeedStateMachine {
    fun computeNewState(
        repoState: PocketVideoRepo.FeedState,
        cacheState: PocketVideoRepo.FeedState?
    ): PocketVideoRepo.FeedState {
        return when {
            repoState === Inactive || cacheState === Inactive -> Inactive
            repoState is LoadComplete -> repoState
            repoState === Loading && cacheState === FetchFailed -> repoState
            repoState === FetchFailed && cacheState === Loading -> repoState
            cacheState == null -> repoState
            else -> cacheState
        }
    }
}
