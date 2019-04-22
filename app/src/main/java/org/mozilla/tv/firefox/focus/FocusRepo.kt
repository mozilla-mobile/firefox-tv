/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.focus

import android.view.View
import androidx.annotation.VisibleForTesting
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class FocusRepo(
    screenController: ScreenController,
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    pocketRepo: PocketVideoRepo) {

    val focusedView: BehaviorSubject<View> = BehaviorSubject.create() // TODO: potential for telemetry?

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

        if (!focusedView.hasValue()) {
            // TODO: handle focus lost
        }

        val currFocusedView: View = focusedView.value!!

        when (activeScreen) {
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY -> {
                when (currFocusedView.id) {
                    R.id.navUrlInput ->
                        updateNavUrlInputFocus(currFocusedView, sessionState, pinnedTilesIsEmpty, pocketState)
                    R.id.navButtonReload -> {

                    }
                    R.id.navButtonForward -> {

                    }
                    R.id.pocketVideoMegaTileView -> {

                    }
                }
            }
            ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> Unit
            ScreenControllerStateMachine.ActiveScreen.POCKET -> Unit
            ScreenControllerStateMachine.ActiveScreen.SETTINGS -> Unit
        }

    }

    private fun updateNavUrlInputFocus(
        focusedNavUrlInputView: View,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState) {

        assert(focusedNavUrlInputView.id == R.id.navUrlInput)

        focusedNavUrlInputView.nextFocusDownId = when {
            pocketState is PocketVideoRepo.FeedState.FetchFailed -> R.id.megaTileTryAgainButton
            pocketState is PocketVideoRepo.FeedState.Inactive -> {
                if (pinnedTilesIsEmpty) {
                    R.id.navUrlInput // if
                } else {
                    R.id.tileContainer
                }
            }
            else -> R.id.pocketVideoMegaTileView
        }

        focusedNavUrlInputView.nextFocusUpId = when {
            sessionState.backEnabled -> R.id.navButtonBack
            sessionState.forwardEnabled -> R.id.navButtonForward
            sessionState.currentUrl != URLs.APP_URL_HOME -> R.id.navButtonReload
            else -> R.id.turboButton
        }
    }
}
