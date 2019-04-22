/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.focus

import android.view.View
import android.view.ViewTreeObserver
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
    pocketRepo: PocketVideoRepo): ViewTreeObserver.OnGlobalFocusChangeListener {

    private var focusedView: BehaviorSubject<View> = BehaviorSubject.create() // TODO: potential for telemetry?

    private val _transition = Observables.combineLatest(
            focusedView,
            screenController.currentActiveScreen,
            sessionRepo.state,
            pinnedTileRepo.isEmpty,
            pocketRepo.feedState) { focusedView, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState ->
        dispatchFocusUpdates(focusedView, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState)
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        newFocus?.apply {
            focusedView.onNext(this)
        }
    }

    @VisibleForTesting
    private fun dispatchFocusUpdates(
        focusedView: View,
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState) {

        when (activeScreen) {
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY -> {
                when (focusedView.id) {
                    R.id.navUrlInput ->
                        updateNavUrlInputFocus(focusedView, sessionState, pinnedTilesIsEmpty, pocketState)
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
