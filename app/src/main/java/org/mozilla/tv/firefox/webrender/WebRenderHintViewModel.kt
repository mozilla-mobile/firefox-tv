/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ext.isUriYouTubeTvVideo
import org.mozilla.tv.firefox.hint.HintContent
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.webrender.cursor.CursorEvent.CursorMoved
import org.mozilla.tv.firefox.webrender.cursor.CursorEvent.ScrolledToEdge
import org.mozilla.tv.firefox.webrender.cursor.CursorModel

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * See comment on [OverlayHintViewModel] for why this is split into two classes.
 */
class WebRenderHintViewModel(
    sessionRepo: SessionRepo,
    cursorModel: CursorModel,
    screenController: ScreenController,
    openMenuHint: HintContent
) : ViewModel(), HintViewModel {

    override val isDisplayed: Observable<Boolean> by lazy {
        sessionRepo.state
                .map { it.currentUrl }
                .distinctUntilChanged()
                .switchMap { url ->
                    return@switchMap when {
                        url.isUriYouTubeTvVideo -> isDisplayedYouTubeVideo
                        else -> isDisplayedOther
                    }
                }
                .distinctUntilChanged()
                .replay(1)
                .autoConnect(0)
    }
    override val hints: Observable<List<HintContent>> = Observable.just(listOf(openMenuHint))

    /**
     * Whether or not the hint bar should be displayed when the user is on a YouTube TV video
     *
     * The hint bar is annoying on YouTube videos, where we have no good way to close it. We hide
     * the hint bar here completely
     */
    private val isDisplayedYouTubeVideo: Observable<Boolean> =
            Observable.just(false)

    /**
     * Whether or not the hint bar should be displayed when the user is on any site other than
     * a YouTube TV video
     */
    private val isDisplayedOther: Observable<Boolean> by lazy {
        val showEvents = Observable.merge(
                webRenderOpened,
                loadComplete,
                scrollUpOrDownToEndOfDom
        ).map { true }

        val hideEvents = cursorMovedUpOrDown
                .map { false }

        Observable.merge(showEvents, hideEvents)
    }

    private val webRenderOpened = screenController.currentActiveScreen
                .filter { it == ScreenControllerStateMachine.ActiveScreen.WEB_RENDER }

    private val loadComplete by lazy {
        fun Observable<SessionRepo.State>.onlyEmitWhenLoadingChanges() =
                this.map { it.loading }
                    .distinctUntilChanged()

        fun Observable<Boolean>.filterLoadComplete() =
                this.filter { loading -> !loading }

        sessionRepo.state
                .filter { it.currentUrl != URLs.APP_URL_HOME }
                .onlyEmitWhenLoadingChanges()
                .filterLoadComplete()
    }

    private val scrollUpOrDownToEndOfDom = cursorModel.cursorMovedEvents
            .ofType(ScrolledToEdge::class.java)
            .filter { it.edge == Direction.UP || it.edge == Direction.DOWN }

    private val cursorMovedUpOrDown = cursorModel.cursorMovedEvents
            .ofType(CursorMoved::class.java)
            .filter { it.direction == Direction.UP || it.direction == Direction.DOWN }
}
