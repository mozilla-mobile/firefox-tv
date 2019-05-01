/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.focus

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.ext.validateKnownViewById
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class FocusRepo(
    screenController: ScreenController,
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    pocketRepo: PocketVideoRepo
) : ViewTreeObserver.OnGlobalFocusChangeListener {

    data class State(
        val focusNode: FocusNode,
        val focused: Boolean = true
    )

    /**
     * FocusNode describes quasi-directional focusable paths given viewId
     */
    data class FocusNode(
        val viewId: Int,
        val nextFocusUpId: Int? = null,
        val nextFocusDownId: Int? = null,
        val nextFocusLeftId: Int? = null,
        val nextFocusRightId: Int? = null
    ) {
        fun updateViewNodeTree(view: View) {
            assert(view.id == viewId)

            nextFocusUpId?.let { view.nextFocusUpId = it }
            nextFocusDownId?.let { view.nextFocusDownId = it }
            nextFocusLeftId?.let { view.nextFocusLeftId = it }
            nextFocusRightId?.let { view.nextFocusRightId = it }
        }
    }

    private val NO_FOCUS_REQUEST = FocusNode(View.NO_ID)

    val defaultViewAfterScreenChange: Observable<Pair<FocusNode, ActiveScreen>> =
            screenController.currentActiveScreen
                    .startWith(ActiveScreen.NAVIGATION_OVERLAY)
                    .buffer(2, 1) // This emits a list of the previous and current screen. See RxTest.kt
                    .filter { (previousScreen, currentsScreen) ->
                        previousScreen != currentsScreen
                    }
                    .map { (previousScreen, currentScreen) ->
                        val focusRequest = when (currentScreen) {
                            ActiveScreen.NAVIGATION_OVERLAY -> {
                                when (previousScreen) {
                                    ActiveScreen.WEB_RENDER -> FocusNode(R.id.navUrlInput)
                                    ActiveScreen.POCKET -> FocusNode(R.id.pocketVideoMegaTileView)
                                    ActiveScreen.SETTINGS -> FocusNode(R.id.settings_tile_telemetry)
                                    else -> NO_FOCUS_REQUEST
                                }
                            }
                            ActiveScreen.WEB_RENDER -> FocusNode(R.id.engineView)
                            ActiveScreen.POCKET -> FocusNode(R.id.videoFeed)
                            ActiveScreen.SETTINGS -> NO_FOCUS_REQUEST
                            else -> NO_FOCUS_REQUEST
                        }
                        return@map focusRequest to currentScreen
                    }
                    .filter { it.first != NO_FOCUS_REQUEST }
                    .replay(1)
                    .autoConnect(0)

    // TODO: potential for telemetry?
    private val _focusedView: BehaviorSubject<State> = BehaviorSubject.createDefault(
            State(FocusNode(R.id.navUrlInput), focused = true)
    )

    private val focusedView = _focusedView
            .distinctUntilChanged()
            .hide()

    val focusUpdate: Observable<State> = Observables.combineLatest(
            focusedView,
            screenController.currentActiveScreen,
            sessionRepo.state,
            pinnedTileRepo.isEmpty,
            pocketRepo.feedState) { state, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState ->
        updateFocusStateIfNew(state, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState)
    }
    .distinctUntilChanged()

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        newFocus?.let { newView ->
            val viewId = newView.validateKnownViewById()

            val newState = State(
                focusNode = FocusNode(viewId),
                focused = true)

            _focusedView.onNext(newState)
        }
    }

    /**
     * Majority of this logic is a result of initial refactoring from NavigationOverlay (mainly
     * to centralize the focus behavior in one place). We may be making fragile assumptions on both
     * programmatical and UX sides, so there may be some problems to be addressed in the future.
     */
    @Suppress("UNREACHABLE_CODE")
    @VisibleForTesting
    private fun updateFocusStateIfNew(
        newState: State,
        activeScreen: ActiveScreen,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState
    ): State {
        when (activeScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                val focusNode = newState.focusNode
                when (focusNode.viewId) {
                    R.id.navUrlInput ->
                        return getNavUrlInputFocusState(
                                focusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                    R.id.navButtonReload -> {
                        return getReloadButtonFocusState(focusNode, sessionState)
                    }
                    R.id.navButtonForward -> {
                        return getForwardButtonFocusState(focusNode, sessionState)
                    }
                    R.id.pocketVideoMegaTileView -> {
                        return getPocketMegaTileFocusState(focusNode, pinnedTilesIsEmpty)
                    }
                    R.id.megaTileTryAgainButton -> {
                        return handleLostFocusInOverlay(
                                focusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                    }
                    R.id.home_tile -> {
                        // If pinnedTiles is empty and current focus is on home_tile, we need to
                        // restore lost focus (this happens when you remove all tiles in the overlay)
                        if (pinnedTilesIsEmpty) {
                            return handleLostFocusInOverlay(
                                    focusNode,
                                    sessionState,
                                    pinnedTilesIsEmpty,
                                    pocketState)
                        }
                    }
                    View.NO_ID -> {
                        // Focus is lost so default it to navUrlInput and set focused = false
                        val newFocusNode = FocusNode(R.id.navUrlInput)

                        return getNavUrlInputFocusState(
                                newFocusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState,
                                focused = false)
                    }
                }
            }
            ActiveScreen.WEB_RENDER -> Unit
            ActiveScreen.POCKET -> Unit
            ActiveScreen.SETTINGS -> Unit
        }

        return newState
    }

    private fun getNavUrlInputFocusState(
        focusedNavUrlInputNode: FocusNode,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState,
        focused: Boolean = true
    ): State {

        assert(focusedNavUrlInputNode.viewId == R.id.navUrlInput)

        val nextFocusDownId = when (pocketState) {
            PocketVideoRepo.FeedState.FetchFailed -> R.id.megaTileTryAgainButton
            PocketVideoRepo.FeedState.Inactive -> {
                if (pinnedTilesIsEmpty) {
                    R.id.settingsTileContainer
                } else {
                    R.id.tileContainer
                }
            }
            is PocketVideoRepo.FeedState.LoadComplete,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey -> R.id.pocketVideoMegaTileView
        }

        val nextFocusUpId = when {
            sessionState.backEnabled -> R.id.navButtonBack
            sessionState.forwardEnabled -> R.id.navButtonForward
            // TODO: this is a duplicating existing logic in the ToolbarVM, may fall out of sync
            sessionState.currentUrl != URLs.APP_URL_HOME -> R.id.navButtonReload
            else -> R.id.turboButton
        }

        return State(
            focusNode = FocusNode(
                focusedNavUrlInputNode.viewId,
                nextFocusUpId,
                nextFocusDownId),
            focused = focused)
    }

    private fun getReloadButtonFocusState(
        focusedReloadButtonNode: FocusNode,
        sessionState: SessionRepo.State
    ): State {

        assert(focusedReloadButtonNode.viewId == R.id.navButtonReload)

        val nextFocusLeftId = when {
            sessionState.forwardEnabled -> R.id.navButtonForward
            sessionState.backEnabled -> R.id.navButtonBack
            else -> R.id.navButtonReload
        }

        return State(
            focusNode = FocusNode(
                focusedReloadButtonNode.viewId,
                nextFocusLeftId = nextFocusLeftId))
    }

    private fun getForwardButtonFocusState(
        focusedForwardButtonNode: FocusNode,
        sessionState: SessionRepo.State
    ): State {

        assert(focusedForwardButtonNode.viewId == R.id.navButtonForward)

        val nextFocusLeftId = when {
            sessionState.backEnabled -> R.id.navButtonBack
            else -> R.id.navButtonForward
        }

        return State(
            focusNode = FocusNode(
                focusedForwardButtonNode.viewId,
                nextFocusLeftId = nextFocusLeftId))
    }

    private fun getPocketMegaTileFocusState(
        focusedPocketMegaTileNode: FocusNode,
        pinnedTilesIsEmpty: Boolean,
        focused: Boolean = true
    ): State {

        assert(focusedPocketMegaTileNode.viewId == R.id.pocketVideoMegaTileView ||
            focusedPocketMegaTileNode.viewId == R.id.megaTileTryAgainButton)

        val nextFocusDownId = when {
            pinnedTilesIsEmpty -> R.id.settingsTileContainer
            else -> R.id.pinned_tiles_channel
        }

        return State(
            focusNode = FocusNode(
                focusedPocketMegaTileNode.viewId,
                nextFocusDownId = nextFocusDownId),
            focused = focused)
    }

    /**
     * Two possible scenarios for losing focus when in overlay:
     * 1. When all the pinned tiles are removed, tilContainer no longer needs focus
     * 2. When click on [megaTileTryAgainButton]
     */
    private fun handleLostFocusInOverlay(
        lostFocusNode: FocusNode,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState
    ): State {

        assert(lostFocusNode.viewId == R.id.tileContainer ||
                lostFocusNode.viewId == R.id.megaTileTryAgainButton)

        val viewId = when (pocketState) {
            PocketVideoRepo.FeedState.FetchFailed -> R.id.megaTileTryAgainButton
            PocketVideoRepo.FeedState.Inactive -> R.id.navUrlInput
            is PocketVideoRepo.FeedState.LoadComplete,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey -> R.id.pocketVideoMegaTileView
        }

        val newFocusNode = FocusNode(viewId)

        // If new focusNode is different from previous, set focused state to false
        val focused = newFocusNode.viewId == lostFocusNode.viewId

        return if (newFocusNode.viewId == R.id.navUrlInput) {
            getNavUrlInputFocusState(newFocusNode, sessionState, pinnedTilesIsEmpty, pocketState, focused)
        } else {
            getPocketMegaTileFocusState(newFocusNode, pinnedTilesIsEmpty, focused)
        }
    }
}
