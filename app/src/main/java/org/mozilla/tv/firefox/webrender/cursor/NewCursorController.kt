/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.DIRECTION_KEY_CODES
import org.mozilla.tv.firefox.utils.Direction

private const val BASE_SPEED = 5f
private const val MAX_VELOCITY = 25f
private const val MS_TO_MAX_ACCELERATION = 200
private const val MAX_ACCELERATION = MAX_VELOCITY - BASE_SPEED
private const val ACCELERATION_PER_MS = MAX_ACCELERATION / MS_TO_MAX_ACCELERATION
private const val MS_PER_FRAME = 16

data class HandleKeyEventResponse(val wasHandled: Boolean, val forwardedMotionEvent: MotionEvent?)

/**
 *  TODO
 *  [X] Remove old Rx implementation cruft
 *  [-] ~Pipe events up to repo (CursorEventRepo is already dipping into key events.  Choose one of these approaches and get rid of the other)~ We cut this as out of scope
 *  [X] Handle visibility animation
 *  [X] Hook up directionKeyPress
 *  [X] Enable/disable invalidate calls
 *  [X] Touch simulation
 *  [X] View click animation
 *  [X] Tweak values to make them feel good
 *  [X] Cursor visible on select press
 *  [ ] General cleanup
 *  [ ] Commit cleanup
 *  [X] MainActivity shouldn't decide what this class handles, this class should. Update that
 *  [X] Make cursor start in center of screen
 *  [ ] Hook up scrolling
 *  [ ] Make sure hint bar still works
 */
class NewCursorController(
        activeScreen: Observable<ScreenControllerStateMachine.ActiveScreen>,
        frameworkRepo: FrameworkRepo,
        sessionRepo: SessionRepo
) {
    var screenBounds: PointF? = null

    private val directionKeysPressed = mutableSetOf<Direction>()
    private var lastVelocity = 0f
    private var lastUpdatedAtMS = 0L // the first value when we start drawing will be an edge case
    private var lastKnownCursorPos = PointF(0f, 0f)
    private var cursorHasBeenCentered = false

    private val _isCursorMoving = BehaviorSubject.createDefault<Boolean>(false)
    val isCursorMoving: Observable<Boolean> = _isCursorMoving.hide()

    private val _isSelectPressed = BehaviorSubject.createDefault<Boolean>(false)
    val isSelectPressed: Observable<Boolean> = _isSelectPressed.hide()

    val isCursorActive: Observable<Boolean> = Observables.combineLatest(
            activeScreen,
            frameworkRepo.isVoiceViewEnabled,
            sessionRepo.state
    ) { activeScreen, isVoiceViewEnabled, sessionState ->
        // We only display the cursor when the web content is active.
        val isWebRenderActive = activeScreen == WEB_RENDER
        val doesWebpageHaveOwnNavControls = sessionState.currentUrl.isUriYouTubeTV || isVoiceViewEnabled
        isWebRenderActive && !doesWebpageHaveOwnNavControls
    }.also {
        // No need to dispose: this survives for the duration of the app.
        it.subscribe { isCursorActive ->
            if (!isCursorActive) resetCursorSpeed()
        }
    }

    fun handleKeyEvent(event: KeyEvent): HandleKeyEventResponse {
        return when {
            DIRECTION_KEY_CODES.contains(event.keyCode) -> {
                HandleKeyEventResponse(
                        wasHandled = directionKeyPress(event),
                        forwardedMotionEvent = null
                )
            }
            // Center key is used on device, Enter key is used on emulator
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                val motionEvent = selectKeyPress(event)
                HandleKeyEventResponse(motionEvent != null, motionEvent)
            }
            else -> HandleKeyEventResponse(false, null)
        }
    }

    /**
     * TODO
     * @return returns true if the event is consumed
     */
    private fun directionKeyPress(event: KeyEvent): Boolean {
        if (!isCursorActive.blockingFirst()) {
            return false
        }
        require(DIRECTION_KEY_CODES.contains(event.keyCode)) { "Invalid key event passed to CursorController#directionKeyPress: $event" }

        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
            else -> return false
        }
        when (event.action) {
            KeyEvent.ACTION_UP -> directionKeysPressed -= direction
            KeyEvent.ACTION_DOWN -> directionKeysPressed += direction
            else -> return false
        }

        _isCursorMoving.onNext(!directionKeysPressed.isEmpty())

        return true
    }

    /**
     * TODO
     * @return returns a MotionEvent if the event is consumed, null otherwise
     */
    @SuppressLint("Recycle")
    @CheckResult(suggest = "Recycle MotionEvent after use")
    private fun selectKeyPress(event: KeyEvent): MotionEvent? {
        if (!isCursorActive.blockingFirst()) {
            return null
        }
        require(event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) { "Invalid key event passed to CursorController#selectKeyPress: $event" }

        val motionEvent = MotionEvent.obtain(event.downTime, event.eventTime, event.action, lastKnownCursorPos.x, lastKnownCursorPos.y, 0)
        return when (event.action) {
            KeyEvent.ACTION_UP -> {
                _isSelectPressed.onNext(false)
                motionEvent
            }
            KeyEvent.ACTION_DOWN -> {
                _isSelectPressed.onNext(true)
                motionEvent
            }
            else -> null
        }
    }

    /**
     * TODO describe this
     *
     * @return whether or not the view should continue to invalidate itself
     */
    fun mutatePosition(oldPos: PointF): Boolean {
        lastKnownCursorPos = oldPos
        when {
            !cursorHasBeenCentered && screenBounds != null -> {
                oldPos.x = screenBounds!!.x / 2
                oldPos.y = screenBounds!!.y / 2
                cursorHasBeenCentered = true
                return true
            }
            directionKeysPressed.isEmpty() -> {
                // Grab old and new times
                resetCursorSpeed()
                return false
            }
            else -> {
                val currTime = System.currentTimeMillis()
                if (lastUpdatedAtMS == -1L) lastUpdatedAtMS = currTime - MS_PER_FRAME // TODO comment about why we do this
                lastVelocity = internalMutatePositionAndReturnVelocity(
                        oldPos,
                        lastUpdatedAtMS,
                        currTime,
                        lastVelocity,
                        directionKeysPressed
                )
                lastUpdatedAtMS = currTime
                return true
            }
        }
    }

    private fun resetCursorSpeed() { // todo: name
        lastVelocity = BASE_SPEED
        lastUpdatedAtMS = -1
        directionKeysPressed.clear()
    }

    private fun internalMutatePositionAndReturnVelocity(
            oldPos: PointF,
            oldTimeMS: Long,
            newTimeMS: Long,
            oldVelocity: Float,
            directionsPressed: Set<Direction>
    ): Float {
        // directionsPressed empty case is handled in `mutatePosition`
        require(directionsPressed.isNotEmpty())

        val timePassed = newTimeMS - oldTimeMS
        val accelerateBy = ACCELERATION_PER_MS * timePassed
        val velocity = (oldVelocity + accelerateBy).coerceIn(0f, MAX_VELOCITY)

        var verticalVelocity = 0f
        if (directionKeysPressed.contains(Direction.UP)) verticalVelocity -= velocity
        if (directionKeysPressed.contains(Direction.DOWN)) verticalVelocity += velocity
        var horizontalVelocity = 0f
        if (directionKeysPressed.contains(Direction.LEFT)) horizontalVelocity -= velocity
        if (directionKeysPressed.contains(Direction.RIGHT)) horizontalVelocity += velocity
        oldPos.y += verticalVelocity
        oldPos.x += horizontalVelocity
        oldPos.x = oldPos.x.coerceIn(0f, screenBounds?.x) // TODO Comment about screenbounds nullability
        oldPos.y = oldPos.y.coerceIn(0f, screenBounds?.y)
        println("SEVTEST: pos.x: ${oldPos.x}, pos.y: ${oldPos.y}")
        return velocity
    }
}
