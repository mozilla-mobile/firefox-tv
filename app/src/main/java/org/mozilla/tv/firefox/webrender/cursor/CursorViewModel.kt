/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.LiveDataCombiners
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.ext.use
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo

private const val DOWN_TIME_OFFSET_MILLIS = 100

/**
 * A [ViewModel] representing the spatial, d-pad cursor used to navigate web pages.
 */
class CursorViewModel(
    frameworkRepo: FrameworkRepo,
    screenController: ScreenController,
    sessionRepo: SessionRepo
) : ViewModel() {
    private val _touchSimulationLiveData = MutableLiveData<Consumable<MotionEvent>>()
    val touchSimulationLiveData: LiveData<Consumable<MotionEvent>> = _touchSimulationLiveData

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

    /**
     * Dispatches a touch event on the current position, sending a click where the cursor is.
     *
     * FIXME: #1828
     * Incremental work: [pos] comes from [LegacyCursorViewModel] => Look to move [pos]
     * manipulation work to [CursorViewModel]
     */
    fun onSelectKeyEvent(action: Int, pos: PointF) {
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now - DOWN_TIME_OFFSET_MILLIS, now, action, pos.x, pos.y, 0).use {
            _touchSimulationLiveData.value = Consumable.from(it)
        }
    }
}
