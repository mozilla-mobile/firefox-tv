/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.focus.FocusRepo

class WebRenderViewModel(focusRepo: FocusRepo, activeScreen: Observable<ScreenControllerStateMachine.ActiveScreen>) : ViewModel() {

    val focusRequest: Observable<Int> = focusRepo.events.withLatestFrom(activeScreen)
            .filter { (_, activeScreen) ->
                activeScreen == ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
            }.map {
                R.id.engineView
            }
}
