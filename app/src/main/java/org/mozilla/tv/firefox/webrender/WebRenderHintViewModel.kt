/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

private val OPEN_MENU_HINT =  Hint(
        R.string.hint_press_menu_to_open_overlay,
        R.string.hardware_button_a11y_menu,
        R.drawable.hardware_remote_menu
)

private enum class Event(val display: Boolean) {
    CURSOR_MOVED_DOWN(false),
    BOTTOM_OF_PAGE_REACHED(true),
    TOP_OF_PAGE_REACHED(true),
    LOAD_COMPLETE(true)
}

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * See comment on [OverlayHintViewModel] for why this is split into two classes.
 */
class WebRenderHintViewModel(sessionRepo: SessionRepo) : ViewModel(), HintViewModel {

    override val isDisplayed: Observable<Boolean> by lazy {
        Observable.merge(cursorEvents, loadCompleteEvents)
                .map { it.display }
                .startWith(false)
                .replay(1)
                .autoConnect(0)
    }
    override val hints: Observable<List<Hint>> = Observable.just(listOf(OPEN_MENU_HINT))

    private val cursorEvents: Subject<Event> = PublishSubject.create<Event>()

    // TODO a lot of this implementation may change based on the response to
    // https://github.com/mozilla-mobile/firefox-tv/issues/1907#issuecomment-474097863
    private val loadCompleteEvents = sessionRepo.state
            .filter { it.currentUrl != URLs.APP_URL_HOME }
            .map { it.loading }
            .distinctUntilChanged()
            .filter { !it }
            .map { Event.LOAD_COMPLETE }

    // TODO how do we prevent cursorMovedDown from overwriting cursorReachedBottomOfPage?
    fun cursorMovedDown() = cursorEvents.onNext(Event.CURSOR_MOVED_DOWN)

    fun cursorReachedTopOfPage() = cursorEvents.onNext(Event.TOP_OF_PAGE_REACHED)

    fun cursorReachedBottomOfPage() = cursorEvents.onNext(Event.BOTTOM_OF_PAGE_REACHED)
}
