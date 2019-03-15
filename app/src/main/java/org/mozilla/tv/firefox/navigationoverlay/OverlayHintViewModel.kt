/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import io.reactivex.Observable
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.webrender.WebRenderHintViewModel

/**
 * Contains business logic for, and exposes data to the hint bar.
 *
 * Although the exposed data is the same between this and [WebRenderHintViewModel],
 * the business logic, dependencies, and API are all substantially different. As
 * the exposed data is the most trivial part of the implementation, these were
 * broken into two classes.
 */
class OverlayHintViewModel(sessionRepo: SessionRepo) {
    // TODO this will require an additional dependency when overlay hint is updated
    // to change contextually according to the currently focused view

    val isDisplayed: Observable<Boolean> = Observable.just(false)
    val hints: Observable<List<Hint>> = Observable.empty()
}
