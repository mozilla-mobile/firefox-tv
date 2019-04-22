/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.focus

import androidx.annotation.VisibleForTesting
import io.reactivex.rxkotlin.Observables
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.session.SessionRepo

class FocusRepo(
    screenController: ScreenController,
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    pocketRepo: PocketVideoRepo) {

    private val _transition = Observables.combineLatest(
        screenController.currentActiveScreen,
        sessionRepo.state,
        pinnedTileRepo.isEmpty,
        pocketRepo.feedState) { activeScreen, sessionState, pinnedTilesIsEmpty, pocketState ->
        dispatchFocusUpdates(activeScreen, sessionState, pinnedTilesIsEmpty, pocketState)
    }

    @VisibleForTesting
    private fun dispatchFocusUpdates(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState) {

        when (activeScreen) {
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY -> Unit
            ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> Unit
            ScreenControllerStateMachine.ActiveScreen.POCKET -> Unit
            ScreenControllerStateMachine.ActiveScreen.SETTINGS -> Unit
        }

    }
}
