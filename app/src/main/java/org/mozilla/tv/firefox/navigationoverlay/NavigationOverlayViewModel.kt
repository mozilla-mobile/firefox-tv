/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.focus.FocusRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class NavigationOverlayViewModel(sessionRepo: SessionRepo, focusRepo: FocusRepo) : ViewModel() {

    val focusRequest: Observable<Int> = focusRepo.events.withLatestFrom(focusRepo.focusUpdate)
        .filter { (_, state) ->
            state.activeScreen == ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY
        }
        .map { (event, state) ->
            when (event) {
                FocusRepo.Event.ScreenChange ->
                    state.defaultFocusMap[state.activeScreen] ?: R.id.navUrlInput
                FocusRepo.Event.RequestFocus ->
                    state.focusNode.viewId
            }
    }

    val focusUpdate: Observable<FocusRepo.FocusNode> = focusRepo.focusUpdate.map { it.focusNode }

    @Suppress("DEPRECATION")
    val viewIsSplit: LiveData<Boolean> = sessionRepo.legacyState.map {
        it.currentUrl != URLs.APP_URL_HOME
    }
}
