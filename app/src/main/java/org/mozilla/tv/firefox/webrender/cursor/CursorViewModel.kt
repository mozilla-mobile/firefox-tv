/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.PointF
import android.os.SystemClock
import android.support.annotation.VisibleForTesting
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.LiveDataCombiners
import org.mozilla.tv.firefox.ext.forceExhaustive
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
    private var prevDownMotionEvent: MotionEvent? = null

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
        validateMotionEvent(action, SystemClock.uptimeMillis(), pos)?.use {
            _touchSimulationLiveData.value = Consumable.from(it)
        }
    }

    /**
     * Problem: there is a distinctive time difference between [MotionEvent.ACTION_DOWN] and
     * [MotionEvent.ACTION_UP] when using DPAD_SELECT_KEY. [GeckoSession] requires them to have matching
     * [MotionEvent.getEventTime] in order to register such [KeyEvent] as "Click" touch event.
     *
     * [validateMotionEvent] checks for previous ACTION_DOWN vs. upcoming ACTION_UP to calculate
     * the diff against [ViewConfiguration.getLongPressTimeout] to determine whether "Click" or
     * "Long Press" event. Also filters unnecessary [MotionEvent] that occurs when holding down
     * DPAD_SELECT_KEY (see (*)) by bypassing consecutive ACTION_DOWN calls.
     *
     * (*) Capture the first instance of [KeyEvent.ACTION_DOWN] upon long press and check against
     * the first instance of [KeyEvent.ACTION_UP].
     */
    @VisibleForTesting
    private fun validateMotionEvent(action: Int, now: Long, pos: PointF) : MotionEvent? {
        var curr = MotionEvent.obtain(now - DOWN_TIME_OFFSET_MILLIS, now, action, pos.x, pos.y, 0)

        // ACTION_DOWN == 0, ACTION_UP == 1
        Log.d("ValidatedMotionEvent", String.format("curr: %d, prev: %d", action, prevDownMotionEvent?.action))

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                if (prevDownMotionEvent == null) {
                    prevDownMotionEvent = MotionEvent.obtain(curr)
                } else {
                    return null
                }
            }
            KeyEvent.ACTION_UP -> {
                if (prevDownMotionEvent != null) {
                    // Record the time diff between curr and prev event event times (>500 => LONG_PRESS)
                    Log.d("updatedMotionEvent", String.format("%d", curr.eventTime - prevDownMotionEvent!!.eventTime))
                    if (curr.eventTime - prevDownMotionEvent!!.eventTime < ViewConfiguration.getLongPressTimeout()) {
                        // Register it as a "Click" Event and match the down & event time with the previous event
                        curr = MotionEvent.obtain(prevDownMotionEvent!!.downTime, prevDownMotionEvent!!.eventTime, action, pos.x, pos.y, 0)
                    }

                    prevDownMotionEvent!!.recycle()
                    prevDownMotionEvent = null
                } else {
                    return null
                }
            }
            else -> {
                // Fallback Option - See [MotionEvent#Consistency Guarantees]
                prevDownMotionEvent!!.recycle()
                prevDownMotionEvent = null
            }
        }.forceExhaustive

        return curr
    }
}
