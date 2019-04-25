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
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

private const val INVALID_VIEW_ID = -1

class FocusRepo(
    screenController: ScreenController,
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    pocketRepo: PocketVideoRepo
) : ViewTreeObserver.OnGlobalFocusChangeListener {

    data class State(
        val activeScreen: ScreenControllerStateMachine.ActiveScreen,
        val focusNode: FocusNode,
        val defaultFocusMap: HashMap<ScreenControllerStateMachine.ActiveScreen, Int>
    )

    enum class Event {
        ScreenChange,
        RequestFocus // to handle lost focus
    }

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

    // TODO: potential for telemetry?
    private val _state: BehaviorSubject<State> = BehaviorSubject.create()

    private val _events: Subject<Event> = PublishSubject.create()
    val events: Observable<Event> = _events.hide()

    // Keep track of prevScreen to identify screen transitions
    private var prevScreen: ScreenControllerStateMachine.ActiveScreen =
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY

    private val _focusUpdate = Observables.combineLatest(
            _state,
            screenController.currentActiveScreen,
            sessionRepo.state,
            pinnedTileRepo.isEmpty,
            pocketRepo.feedState) { state, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState ->
        dispatchFocusUpdates(state.focusNode, activeScreen, sessionState, pinnedTilesIsEmpty, pocketState)
    }

    val focusUpdate: Observable<State> = _focusUpdate.hide()

    init {
        initializeDefaultFocus()
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        newFocus?.let {
            val newState = State(
                activeScreen = _state.value!!.activeScreen,
                focusNode = FocusNode(it.id),
                defaultFocusMap = _state.value!!.defaultFocusMap)

            _state.onNext(newState)
        }
    }

    private fun initializeDefaultFocus() {
        val focusMap = HashMap<ScreenControllerStateMachine.ActiveScreen, Int>()
        focusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] = R.id.navUrlInput
        focusMap[ScreenControllerStateMachine.ActiveScreen.WEB_RENDER] = R.id.engineView
        focusMap[ScreenControllerStateMachine.ActiveScreen.POCKET] = R.id.videoFeed

        val newState = State(
            activeScreen = ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY,
            focusNode = FocusNode(R.id.navUrlInput),
            defaultFocusMap = focusMap)

        _state.onNext(newState)
    }

    @VisibleForTesting
    private fun dispatchFocusUpdates(
        focusNode: FocusNode,
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState
    ): State {

        var newState = _state.value!!
        val focusMap = _state.value!!.defaultFocusMap
        when (activeScreen) {
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY -> {

                // Check previous screen for defaultFocusMap updates
                when (prevScreen) {
                    ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> {
                        newState = updateDefaultFocusForOverlayWhenTransitioningFromWebRender(
                                activeScreen,
                                focusMap,
                                sessionState)
                    }
                    ScreenControllerStateMachine.ActiveScreen.POCKET -> {
                        newState = updateDefaultFocusForOverlayWhenTransitioningFromPocket(activeScreen, focusMap)
                    }
                    else -> Unit
                }

                when (focusNode.viewId) {
                    R.id.navUrlInput ->
                        newState = updateNavUrlInputFocusTree(
                                activeScreen,
                                focusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                    R.id.navButtonReload -> {
                        newState = updateReloadButtonFocusTree(activeScreen, focusNode, sessionState)
                    }
                    R.id.navButtonForward -> {
                        newState = updateForwardButtonFocusTree(activeScreen, focusNode, sessionState)
                    }
                    R.id.pocketVideoMegaTileView -> {
                        newState = updatePocketMegaTileFocusTree(activeScreen, focusNode, pinnedTilesIsEmpty)
                    }
                    R.id.megaTileTryAgainButton -> {
                        newState = handleLostFocusInOverlay(
                                activeScreen,
                                focusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                    }
                    R.id.home_tile -> {
                        // If pinnedTiles is empty and current focus is on home_tile, we need to
                        // restore lost focus (this happens when you remove all tiles in the overlay)
                        if (pinnedTilesIsEmpty) {
                            newState = handleLostFocusInOverlay(
                                    activeScreen,
                                    focusNode,
                                    sessionState,
                                    pinnedTilesIsEmpty,
                                    pocketState)
                        }
                    }
                    INVALID_VIEW_ID -> {
                        // Focus is lost so default it to navUrlInput and send a [Event.RequestFocus]
                        val newFocusNode = FocusNode(R.id.navUrlInput)

                        newState = updateNavUrlInputFocusTree(
                                activeScreen,
                                newFocusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                        _events.onNext(Event.RequestFocus)
                    }
                }
            }
            ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> {}
            ScreenControllerStateMachine.ActiveScreen.POCKET -> {}
            ScreenControllerStateMachine.ActiveScreen.SETTINGS -> Unit
        }

        if (prevScreen != activeScreen) {
            _events.onNext(Event.ScreenChange)
            prevScreen = activeScreen
        }

        return newState
    }

    private fun updateNavUrlInputFocusTree(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        focusedNavUrlInputNode: FocusNode,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState
    ): State {

        assert(focusedNavUrlInputNode.viewId == R.id.navUrlInput)

        val nextFocusDownId = when {
            pocketState is PocketVideoRepo.FeedState.FetchFailed -> R.id.megaTileTryAgainButton
            pocketState is PocketVideoRepo.FeedState.Inactive -> {
                if (pinnedTilesIsEmpty) {
                    R.id.navUrlInput
                } else {
                    R.id.tileContainer
                }
            }
            else -> R.id.pocketVideoMegaTileView
        }

        val nextFocusUpId = when {
            sessionState.backEnabled -> R.id.navButtonBack
            sessionState.forwardEnabled -> R.id.navButtonForward
            sessionState.currentUrl != URLs.APP_URL_HOME -> R.id.navButtonReload
            else -> R.id.turboButton
        }

        return State(
            activeScreen = activeScreen,
            focusNode = FocusNode(
                focusedNavUrlInputNode.viewId,
                nextFocusUpId,
                nextFocusDownId),
            defaultFocusMap = _state.value!!.defaultFocusMap)
    }

    private fun updateReloadButtonFocusTree(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
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
            activeScreen = activeScreen,
            focusNode = FocusNode(
                focusedReloadButtonNode.viewId,
                nextFocusLeftId = nextFocusLeftId),
            defaultFocusMap = _state.value!!.defaultFocusMap)
    }

    private fun updateForwardButtonFocusTree(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        focusedForwardButtonNode: FocusNode,
        sessionState: SessionRepo.State
    ): State {

        assert(focusedForwardButtonNode.viewId == R.id.navButtonForward)

        val nextFocusLeftId = when {
            sessionState.backEnabled -> R.id.navButtonBack
            else -> R.id.navButtonForward
        }

        return State(
            activeScreen = activeScreen,
            focusNode = FocusNode(
                focusedForwardButtonNode.viewId,
                nextFocusLeftId = nextFocusLeftId),
            defaultFocusMap = _state.value!!.defaultFocusMap)
    }

    private fun updatePocketMegaTileFocusTree(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        focusedPocketMegatTileNode: FocusNode,
        pinnedTilesIsEmpty: Boolean
    ): State {

        assert(focusedPocketMegatTileNode.viewId == R.id.pocketVideoMegaTileView ||
            focusedPocketMegatTileNode.viewId == R.id.megaTileTryAgainButton)

        val nextFocusDownId = when {
            pinnedTilesIsEmpty -> R.id.settingsTileContainer
            else -> R.id.tileContainer
        }

        return State(
            activeScreen = activeScreen,
            focusNode = FocusNode(
                focusedPocketMegatTileNode.viewId,
                nextFocusDownId = nextFocusDownId),
            defaultFocusMap = _state.value!!.defaultFocusMap)
    }

    /**
     * Two possible scenarios for losing focus when in overlay:
     * 1. When all the pinned tiles are removed, tilContainer no longer needs focus
     * 2. When click on [megaTileTryAgainButton]
     */
    private fun handleLostFocusInOverlay(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
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
            else -> R.id.pocketVideoMegaTileView
        }

        val newFocusNode = FocusNode(viewId)
        val newState = if (newFocusNode.viewId == R.id.navUrlInput) {
            updateNavUrlInputFocusTree(activeScreen, newFocusNode, sessionState, pinnedTilesIsEmpty, pocketState)
        } else {
            updatePocketMegaTileFocusTree(activeScreen, newFocusNode, pinnedTilesIsEmpty)
        }

        // Request focus on newState
        if (newFocusNode.viewId != lostFocusNode.viewId) {
            _events.onNext(Event.RequestFocus)
        }

        return newState
    }

    private fun updateDefaultFocusForOverlayWhenTransitioningFromWebRender(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        focusMap: HashMap<ScreenControllerStateMachine.ActiveScreen, Int>,
        sessionState: SessionRepo.State
    ): State {

        // It doesn't make sense to be able to transition to WebRender if currUrl == APP_URL_HOME
        assert(sessionState.currentUrl != URLs.APP_URL_HOME)

        focusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] = when {
            sessionState.backEnabled -> R.id.navButtonBack
            sessionState.forwardEnabled -> R.id.navButtonForward
            else -> R.id.navButtonReload
        }

        return State(
                activeScreen = activeScreen,
                focusNode = _state.value!!.focusNode,
                defaultFocusMap = focusMap)
    }

    private fun updateDefaultFocusForOverlayWhenTransitioningFromPocket(
        activeScreen: ScreenControllerStateMachine.ActiveScreen,
        focusMap: HashMap<ScreenControllerStateMachine.ActiveScreen, Int>
    ): State {
        focusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] =
                R.id.pocketVideoMegaTileView

        return State(
            activeScreen = activeScreen,
            focusNode = _state.value!!.focusNode,
            defaultFocusMap = focusMap)
    }
}
