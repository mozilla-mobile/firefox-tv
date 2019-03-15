/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import io.reactivex.Observable
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.session.SessionRepo

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * See comment on [OverlayHintViewModel] for why this is split into two classes.
 */
class WebRenderHintViewModel(sessionRepo: SessionRepo) {

    val isDisplayed: Observable<Boolean> = Observable.just(false)
    val hints: Observable<List<Hint>> = Observable.empty()

    fun cursorMovedDown() {

    }

    fun cursorReachedTopOfPage() {

    }

    fun cursorReachedBottomOfPage() {

    }
}
