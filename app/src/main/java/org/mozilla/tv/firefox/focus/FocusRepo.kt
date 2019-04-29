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
import mozilla.components.browser.engine.system.NestedWebView
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

private const val NESTED_WEB_VIEW_ID = 2147483646 // Int.MAX_VALUE - 1

class FocusRepo(
    screenController: ScreenController,
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    pocketRepo: PocketVideoRepo
) : ViewTreeObserver.OnGlobalFocusChangeListener {

    val defaultFocusMap: HashMap<ScreenControllerStateMachine.ActiveScreen, Int> = HashMap()

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

    init {
        initializeDefaultFocus()
    }

    private fun initializeDefaultFocus() {
        defaultFocusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] = R.id.navUrlInput
        defaultFocusMap[ScreenControllerStateMachine.ActiveScreen.WEB_RENDER] = R.id.engineView
        defaultFocusMap[ScreenControllerStateMachine.ActiveScreen.POCKET] = R.id.videoFeed
    }

    // TODO: potential for telemetry?
    private val _state: BehaviorSubject<State> = BehaviorSubject.createDefault(
            State(FocusNode(R.id.navUrlInput), focused = true)
    )

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

    val focusUpdate: Observable<State> = _focusUpdate
            .distinctUntilChanged()
            .hide()

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        fun <T> BehaviorSubject<T>.onNextIfNew(value: T) {
            val currState = this.value as State
            val newState = value as State
            if (currState.focusNode.viewId != newState.focusNode.viewId &&
                    newState.focusNode.viewId != NESTED_WEB_VIEW_ID)
                this.onNext(value)
        }

        newFocus?.let { newView ->
            val viewId = validateKnownViewById(newView)

            val newState = State(
                focusNode = FocusNode(viewId),
                focused = true)

            _state.onNextIfNew(newState)
        }
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
        when (activeScreen) {
            ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY -> {

                // Check previous screen for defaultFocusMap updates
                when (prevScreen) {
                    ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> {
                        updateDefaultFocusForOverlayWhenTransitioningFromWebRender(sessionState)
                    }
                    ScreenControllerStateMachine.ActiveScreen.POCKET -> {
                        updateDefaultFocusForOverlayWhenTransitioningFromPocket()
                    }
                    else -> Unit
                }

                when (focusNode.viewId) {
                    R.id.navUrlInput ->
                        newState = updateNavUrlInputFocusTree(
                                focusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState)
                    R.id.navButtonReload -> {
                        newState = updateReloadButtonFocusTree(focusNode, sessionState)
                    }
                    R.id.navButtonForward -> {
                        newState = updateForwardButtonFocusTree(focusNode, sessionState)
                    }
                    R.id.pocketVideoMegaTileView -> {
                        newState = updatePocketMegaTileFocusTree(focusNode, pinnedTilesIsEmpty)
                    }
                    R.id.megaTileTryAgainButton -> {
                        newState = handleLostFocusInOverlay(
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
                                    focusNode,
                                    sessionState,
                                    pinnedTilesIsEmpty,
                                    pocketState)
                        }
                    }
                    View.NO_ID -> {
                        // Focus is lost so default it to navUrlInput and set focused = false
                        val newFocusNode = FocusNode(R.id.navUrlInput)

                        newState = updateNavUrlInputFocusTree(
                                newFocusNode,
                                sessionState,
                                pinnedTilesIsEmpty,
                                pocketState,
                                focused = false)
                    }
                }
            }
            ScreenControllerStateMachine.ActiveScreen.WEB_RENDER -> {}
            ScreenControllerStateMachine.ActiveScreen.POCKET -> {}
            ScreenControllerStateMachine.ActiveScreen.SETTINGS -> Unit
        }

        if (prevScreen != activeScreen) {
            prevScreen = activeScreen
        }

        return newState
    }

    private fun updateNavUrlInputFocusTree(
        focusedNavUrlInputNode: FocusNode,
        sessionState: SessionRepo.State,
        pinnedTilesIsEmpty: Boolean,
        pocketState: PocketVideoRepo.FeedState,
        focused: Boolean = true
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
            focusNode = FocusNode(
                focusedNavUrlInputNode.viewId,
                nextFocusUpId,
                nextFocusDownId),
            focused = focused)
    }

    private fun updateReloadButtonFocusTree(
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

    private fun updateForwardButtonFocusTree(
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

    private fun updatePocketMegaTileFocusTree(
        focusedPocketMegatTileNode: FocusNode,
        pinnedTilesIsEmpty: Boolean,
        focused: Boolean = true
    ): State {

        assert(focusedPocketMegatTileNode.viewId == R.id.pocketVideoMegaTileView ||
            focusedPocketMegatTileNode.viewId == R.id.megaTileTryAgainButton)

        val nextFocusDownId = when {
            pinnedTilesIsEmpty -> R.id.settingsTileContainer
            else -> R.id.tileContainer
        }

        return State(
            focusNode = FocusNode(
                focusedPocketMegatTileNode.viewId,
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
            else -> R.id.pocketVideoMegaTileView
        }

        val newFocusNode = FocusNode(viewId)

        // If new focusNode is different from previous, set focused state to false
        val focused = newFocusNode.viewId == lostFocusNode.viewId

        return if (newFocusNode.viewId == R.id.navUrlInput) {
            updateNavUrlInputFocusTree(newFocusNode, sessionState, pinnedTilesIsEmpty, pocketState, focused)
        } else {
            updatePocketMegaTileFocusTree(newFocusNode, pinnedTilesIsEmpty, focused)
        }
    }

    private fun updateDefaultFocusForOverlayWhenTransitioningFromWebRender(
        sessionState: SessionRepo.State
    ) {
        defaultFocusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] = when {
            sessionState.backEnabled -> R.id.navButtonBack
            sessionState.forwardEnabled -> R.id.navButtonForward
            else -> R.id.navButtonReload
        }
    }

    private fun updateDefaultFocusForOverlayWhenTransitioningFromPocket() {
        defaultFocusMap[ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY] =
                R.id.pocketVideoMegaTileView
    }

    /**
     * When view gains focus, its child(ren) views may gain focus with undefined View_ID
     * due to programmatic declaration
     */
    private fun validateKnownViewById(viewToValidate: View): Int {
        if (viewToValidate.id == View.NO_ID) {
            when (viewToValidate) {
                is NestedWebView -> return NESTED_WEB_VIEW_ID
                else -> {
                    // TODO: need sentry/telemetry to keep track of what views without IDs get passed in
                }
            }
        }

        return viewToValidate.id
    }
}
