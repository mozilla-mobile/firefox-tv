/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.graphics.PointF
import android.os.SystemClock
import android.support.annotation.UiThread
import android.support.v4.math.MathUtils
import android.view.MotionEvent
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.isActive
import org.mozilla.tv.firefox.ext.use
import org.mozilla.tv.firefox.utils.Direction
import java.util.EnumSet
import kotlin.properties.Delegates

private const val UPDATE_DELAY_MILLIS = 17L // ~60 FPS.
private const val UPDATE_DELAY_MILLIS_F = UPDATE_DELAY_MILLIS.toFloat() // Avoid conversion in update loop.

private const val ACCEL_MODIFIER = 0.98f
private const val MAX_VELOCITY = 21.25f

private const val DOWN_TIME_OFFSET_MILLIS = 100

/**
 * A model to back the Cursor view.
 *
 * It has the following responsibilities:
 * - Data: stores current cursor position, velocity, etc.
 * - Loop: manage an event loop
 * - Update: updates & clamps (to argument bounds) the Cursor data from the event loop
 * - Notify: tell listeners about data updates, including scroll events
 *
 * When using this class, be sure to update the public properties, e.g. [maxBounds].
 *
 * This ViewModel is written with its lifecycle being the same as its View.
 *
 * @param onUpdate Callback when the state of the cursor is updated: this will be called from the UI thread.
 * @param simulateTouchEvent Takes the given touch event and simulates a touch to the screen.
 */
class CursorViewModel(
    uiLifecycleCancelJob: Job,
    private val onUpdate: (x: Float, y: Float, percentMaxScrollVel: PointF, framesPassed: Float) -> Unit,
    private val simulateTouchEvent: (MotionEvent) -> Unit
) {

    private val scrollVelReturnVal = PointF()

    /**
     * The bounds inside which this cursor should remain, i.e. the screen bounds.
     * Must be set by the caller.
     */
    @set:UiThread
    var maxBounds by Delegates.observable(PointF(0f, 0f)) { _, _, newValue ->
        if (isInitialPosSet) {
            clampPos(pos, newValue)
        } else {
            isInitialPosSet = true
            pos.set(newValue.x / 2, newValue.y / 2) // Center.
        }
        onUpdate(pos.x, pos.y, getPercentMaxScrollVel(), 1f)
    }

    private var isInitialPosSet = false
    private val pos = PointF(0f, 0f)
    private var vel = 0f

    private val pressedDirections = EnumSet.noneOf(Direction::class.java)

    private var updateLoop: Deferred<Unit>? = null

    private val uiScope = CoroutineScope(Dispatchers.Main + uiLifecycleCancelJob)

    private fun update(deltaMillis: Long) {
        /**
         * Gets the amount velocity should change per frame. The algorithm was determined
         * experimentally and does not adhere to kinematic math.
         */
        fun getDeltaVelPerFrame() = (vel + 1) * ACCEL_MODIFIER - vel

        // Frames aren't guaranteed to occur at perfect intervals so we adjust the distance
        // travelled by the amount of time that has actually passed between frames (as
        // opposed to the amount of time we expect to pass between frames): this guarantees equal
        // distance travelled when the system can't keep up with our desired framerate but the
        // cursor will noticeably skip ahead as frames are dropped.
        //
        // This adjustment could be expressed more naturally if this algorithm was expressed
        // as a series of kinematic equations, i.e. vnew = vold + accel * deltaTime.
        val framesPassed = deltaMillis / UPDATE_DELAY_MILLIS_F
        val deltaVelForCurrentUpdate = getDeltaVelPerFrame() * framesPassed

        vel = Math.min(MAX_VELOCITY, vel + deltaVelForCurrentUpdate)

        val isMovingDiagonal = pressedDirections.size > 1
        val finalVel = if (isMovingDiagonal) vel / 2 else vel

        for (dir in pressedDirections) {
            updatePosForVel(dir, finalVel, framesPassed)
        }

        clampPos(pos, maxBounds)
        onUpdate(pos.x, pos.y, getPercentMaxScrollVel(), framesPassed)
    }

    private fun updatePosForVel(dir: Direction, vel: Float, framesPassed: Float) {
        val deltaPosForCurrentUpdate = vel * framesPassed
        when (dir) {
            Direction.UP -> pos.y -= deltaPosForCurrentUpdate
            Direction.DOWN -> pos.y += deltaPosForCurrentUpdate
            Direction.LEFT -> pos.x -= deltaPosForCurrentUpdate
            Direction.RIGHT -> pos.x += deltaPosForCurrentUpdate
        }
    }

    private fun getPercentMaxScrollVel(): PointF {
        scrollVelReturnVal.set(0f, 0f)
        if (vel > 0f) {
            val percentMaxVel = vel / MAX_VELOCITY
            if (pos.x == 0f && pressedDirections.contains(Direction.LEFT)) {
                scrollVelReturnVal.x = -percentMaxVel
            } else if (pos.x == maxBounds.x && pressedDirections.contains(Direction.RIGHT)) {
                scrollVelReturnVal.x = percentMaxVel
            }

            if (pos.y == 0f && pressedDirections.contains(Direction.UP)) {
                scrollVelReturnVal.y = -percentMaxVel
            } else if (pos.y == maxBounds.y && pressedDirections.contains(Direction.DOWN)) {
                scrollVelReturnVal.y = percentMaxVel
            }
        }
        return scrollVelReturnVal
    }

    // TODO: stop when new views (e.g. overlay) are opened.
    private fun asyncStartUpdates() = uiScope.async { // Use UI to avoid synchronization.
        var currentFrameMillis = SystemClock.uptimeMillis() // duped in loop.
        var prevFrameMillis: Long
        var deltaMillis = UPDATE_DELAY_MILLIS // Move ~1 frame to start.
        while (isActive) {
            update(deltaMillis)

            delay(UPDATE_DELAY_MILLIS)

            prevFrameMillis = currentFrameMillis
            currentFrameMillis = SystemClock.uptimeMillis() // duped in init.
            deltaMillis = currentFrameMillis - prevFrameMillis
        }
    }

    fun onDirectionKeyDown(dir: Direction) {
        pressedDirections.add(dir)
        if (updateLoop == null) {
            updateLoop = asyncStartUpdates()
        }
    }

    fun onDirectionKeyUp(dir: Direction) {
        pressedDirections.remove(dir)
        if (pressedDirections.isEmpty()) {
            cancelUpdates()
        }
    }

    /** Dispatches a touch event on the current position, sending a click where the cursor is. */
    fun onSelectKeyEvent(action: Int) {
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now - DOWN_TIME_OFFSET_MILLIS, now, action, pos.x, pos.y, 0).use {
            simulateTouchEvent(it)
        }
    }

    fun cancelUpdates() {
        pressedDirections.clear()
        updateLoop?.cancel()
        updateLoop = null
        vel = 0f // Stop moving.
    }
}

private fun clampPos(pos: PointF, maxBounds: PointF) {
    pos.x = MathUtils.clamp(pos.x, 0f, maxBounds.x)
    pos.y = MathUtils.clamp(pos.y, 0f, maxBounds.y)
}
