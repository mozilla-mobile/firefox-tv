/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.webrender.cursor.CursorEventRepo
import org.mozilla.tv.firefox.webrender.cursor.CursorEventRepo.CursorEvent.CursorMoved
import org.mozilla.tv.firefox.webrender.cursor.CursorEventRepo.CursorEvent.ScrolledToEdge

private val OPEN_MENU_HINT = Hint(
    R.string.hint_press_menu_to_open_overlay,
    R.string.hardware_button_a11y_menu,
    R.drawable.hardware_remote_menu
)

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * See comment on [OverlayHintViewModel] for why this is split into two classes.
 */
class WebRenderHintViewModel(
    sessionRepo: SessionRepo,
    cursorEventRepo: CursorEventRepo,
    screenController: ScreenController
) : ViewModel(), HintViewModel {

    override val isDisplayed by lazy {
        Observable.merge(webRenderOpenedEvents, cursorEvents, loadCompleteEvents)
                .replay(1)
                .autoConnect(0)
    }
    override val hints: Observable<List<Hint>> = Observable.just(listOf(OPEN_MENU_HINT))

    /**
     * Emits true when the hint bar should be shown, or false when it should be hidden
     */
    private val webRenderOpenedEvents by lazy {
        screenController.currentActiveScreen
                .filter { it == ScreenControllerStateMachine.ActiveScreen.WEB_RENDER }
                .map { true }
    }

    /**
     * Emits true when the hint bar should be shown, or false when it should be hidden
     */
    private val loadCompleteEvents by lazy {
        fun Observable<SessionRepo.State>.onlyEmitWhenLoadingChanges() =
                this.map { it.loading }
                        .distinctUntilChanged()

        fun Observable<Boolean>.filterLoadComplete() =
                this.filter { loading -> !loading }

        fun <T> Observable<T>.setShouldDisplay() =
                this.map { true }

        sessionRepo.state
                .filter { it.currentUrl != URLs.APP_URL_HOME }
                .onlyEmitWhenLoadingChanges()
                .filterLoadComplete()
                .setShouldDisplay()
    }

    /**
     * Emits true when the hint bar should be shown, or false when it should be hidden
     */
    private val cursorEvents: Observable<Boolean> by lazy {
        val showOnScrollUpOrDownToEdge = cursorEventRepo.webRenderDirectionEvents
                .ofType(ScrolledToEdge::class.java)
                .filter { it.edge == Direction.UP || it.edge == Direction.DOWN }
                .map { true }

        val hideOnOtherScrollUpOrDown = cursorEventRepo.webRenderDirectionEvents
                .ofType(CursorMoved::class.java)
                .filter { it.direction == Direction.UP || it.direction == Direction.DOWN }
                .map { false }

        Observable.merge(showOnScrollUpOrDownToEdge, hideOnOtherScrollUpOrDown)
    }
}
