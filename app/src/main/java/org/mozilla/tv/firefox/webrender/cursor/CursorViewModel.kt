/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.graphics.PointF
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.lifecycle.LiveDataReactiveStreams
import io.reactivex.BackpressureStrategy
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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val DPAD_LONG_PRESS_TIMEOUT = 1500 // ViewConfiguration.DEFAULT_LONG_PRESS_TIMEOUT * 3

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val DPAD_TAP_TIMEOUT = 300 // ViewConfiguration.TAP_TIMEOUT * 3

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
    private val currentActiveScreen = LiveDataReactiveStreams
            .fromPublisher(screenController.currentActiveScreen.toFlowable(BackpressureStrategy.LATEST))

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
        currentActiveScreen
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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Synchronized
    fun validateMotionEvent(action: Int, eventTime: Long, pos: PointF): MotionEvent? {
        val downTime = eventTime - DOWN_TIME_OFFSET_MILLIS

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                prevDownMotionEvent?.let {
                    if (eventTime - it.eventTime >= DPAD_LONG_PRESS_TIMEOUT) {
                        prevDownMotionEvent = MotionEvent.obtain(downTime, eventTime, action, pos.x, pos.y, 0)
                    } else {
                        return null
                    }
                } ?: let {
                    prevDownMotionEvent = MotionEvent.obtain(downTime, eventTime, action, pos.x, pos.y, 0)
                }

                return prevDownMotionEvent
            }
            KeyEvent.ACTION_UP -> {
                prevDownMotionEvent?.let {
                    if (eventTime - it.eventTime < DPAD_TAP_TIMEOUT) {
                        // Register it as a "Click" Event and match the down & event time with the previous event
                        return MotionEvent.obtain(it.downTime, it.eventTime, action, pos.x, pos.y, 0)
                    } else if (eventTime - it.eventTime < DPAD_LONG_PRESS_TIMEOUT) {
                        return MotionEvent.obtain(downTime, eventTime, action, pos.x, pos.y, 0)
                    }

                    prevDownMotionEvent = null
                } ?: return null
            }
            else -> {
                // Fallback Option - See [MotionEvent#Consistency Guarantees]
                prevDownMotionEvent?.recycle()
                prevDownMotionEvent = null
            }
        }.forceExhaustive

        return null
    }
}
