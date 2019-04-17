/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.ext.toMotionEvent
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

    private val _touchSimulationLiveData = MutableLiveData<Consumable<MotionEvent>>()
    val touchSimulationLiveData: LiveData<Consumable<MotionEvent>> = _touchSimulationLiveData

    val isEnabled: Observable<Boolean> = Observables.combineLatest(
        screenController.currentActiveScreen,
        frameworkRepo.isVoiceViewEnabled,
        sessionRepo.state
    ) { activeScreen, isVoiceViewEnabled, sessionState ->
        val isWebRenderActive = activeScreen == WEB_RENDER // no need to show cursor when engineView is not active.
        val doesWebpageHaveOwnNavControls = sessionState.currentUrl.isUriYouTubeTV || isVoiceViewEnabled

        isWebRenderActive && !doesWebpageHaveOwnNavControls
    }

    /**
     * Dispatches a touch event on the current position, sending a click where the cursor is.
     *
     * FIXME: #1828
     * Incremental work: [pos] comes from [LegacyCursorViewModel] => Look to move [pos]
     * manipulation work to [CursorViewModel]
     */
    fun onSelectKeyEvent(keyEvent: KeyEvent, pos: PointF) {
        _touchSimulationLiveData.value = Consumable.from(keyEvent.toMotionEvent(pos))
    }
}
