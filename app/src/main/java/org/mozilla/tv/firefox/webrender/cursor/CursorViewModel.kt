/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.LiveDataCombiners
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo

/**
 * A [ViewModel] representing the spatial, d-pad cursor used to navigate web pages.
 */
class CursorViewModel(
    frameworkRepo: FrameworkRepo,
    screenController: ScreenController,
    sessionRepo: SessionRepo
) : ViewModel() {

    private val isConfigurationWithOwnNavControls: LiveData<Boolean> = LiveDataCombiners.combineLatest(
        frameworkRepo.isVoiceViewEnabled,
        sessionRepo.state
    ) { isVoiceViewEnabled, sessionState ->
        val isYouTubeTV = sessionState.currentUrl.isUriYouTubeTV
        isYouTubeTV || isVoiceViewEnabled
    }

    // TODO: this complexly combines 3 streams by calling combineLatest twice: consider using a library instead #1783
    val isEnabled: LiveData<Boolean> = LiveDataCombiners.combineLatest(
        isConfigurationWithOwnNavControls,
        screenController.currentActiveScreen
    ) { isConfigurationWithOwnNavControls, activeScreen ->
        val isWebRenderActive = activeScreen == WEB_RENDER
        isWebRenderActive && !isConfigurationWithOwnNavControls
    }
}
