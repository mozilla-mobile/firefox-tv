/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.hint.HintContent
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.webrender.WebRenderHintViewModel

private val CLOSE_MENU_HINT = HintContent(
        R.string.hint_press_back_to_close_overlay,
        R.string.hint_press_back_to_close_overlay_a11y,
        R.drawable.hardware_remote_back
)

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * Although the exposed data is the same between this and [WebRenderHintViewModel],
 * the business logic, dependencies, and API are all substantially different. As
 * the exposed data is the most trivial part of the implementation, these were
 * broken into two classes.
 */
class OverlayHintViewModel(sessionRepo: SessionRepo) : ViewModel(), HintViewModel {
    // TODO this will require an additional dependency when overlay hint is updated
    // to change contextually according to the currently focused view

    override val isDisplayed: Observable<Boolean> = sessionRepo.state
            .map { it.backEnabled }
            .distinctUntilChanged()
            .replay(1)
            .autoConnect(0)
    override val hints: Observable<List<HintContent>> = Observable.just(listOf(CLOSE_MENU_HINT))
}
