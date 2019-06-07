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
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
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
                                    ActiveScreen.SETTINGS -> FocusNode(R.id.settings_tile_telemetry)
                                    else -> NO_FOCUS_REQUEST
                                }
                            }
                            ActiveScreen.WEB_RENDER -> FocusNode(R.id.engineView)
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
                    R.id.turboButton -> {
                        return getTurboButtonFocusState(focusNode, sessionState)
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

        val nextFocusDownId = when {
            pocketState is PocketVideoRepo.FeedState.LoadComplete -> R.id.pocket_channel
            !pinnedTilesIsEmpty -> R.id.pinned_tiles_channel
            else -> R.id.settingsTileContainer
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
                nextFocusUpId = nextFocusUpId,
                nextFocusDownId = nextFocusDownId,
                nextFocusLeftId = R.id.navUrlInput,
                nextFocusRightId = R.id.navUrlInput),
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

    private fun getTurboButtonFocusState(
        focusedTurboButtonNode: FocusNode,
        sessionState: SessionRepo.State
    ): State {

        assert(focusedTurboButtonNode.viewId == R.id.turboButton)

        val nextFocusLeftId = when {
            // TODO: this is a duplicating existing logic in the ToolbarVM, may fall out of sync
            sessionState.currentUrl == URLs.APP_URL_HOME -> {
                if (sessionState.forwardEnabled) {
                    R.id.navButtonForward
                } else {
                    R.id.turboButton
                }
            }
            else -> R.id.pinButton
        }

        return State(
            focusNode = FocusNode(
                focusedTurboButtonNode.viewId,
                nextFocusLeftId = nextFocusLeftId))
    }

    /**
     * Focus can be lost from the overlay under the following conditions:
     * 1. When all the pinned tiles are removed, tilContainer no longer needs focus
     */
    private fun handleLostFocusInOverlay(
        lostFocusNode: FocusNode,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState
    ): State {
        assert(lostFocusNode.viewId == R.id.channelsContainer)

        val viewId = when (pocketState) {
            PocketVideoRepo.FeedState.Inactive,
            PocketVideoRepo.FeedState.NoAPIKey -> R.id.navUrlInput
            is PocketVideoRepo.FeedState.LoadComplete -> R.id.pocket_channel
        }

        val newFocusNode = FocusNode(viewId)

        // If new focusNode is different from previous, set focused state to false
        val focused = newFocusNode.viewId == lostFocusNode.viewId

        return getNavUrlInputFocusState(newFocusNode, sessionState, pinnedTilesIsEmpty, pocketState, focused)
    }
}
