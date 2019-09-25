/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.fxa.FxaLoginUseCase

class WebRenderViewModel(
    screenController: ScreenController,
    fxaLoginUseCase: FxaLoginUseCase
) : ViewModel() {

    val onFxaLoginSuccess = fxaLoginUseCase.onLoginSuccess

    val focusRequests: Observable<Int> = screenController.currentActiveScreen
            .filter { currentScreen -> currentScreen == ActiveScreen.WEB_RENDER }
            .map { R.id.engineView }
}
