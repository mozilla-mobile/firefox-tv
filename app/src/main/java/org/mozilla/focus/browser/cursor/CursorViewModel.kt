/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.cursor

import android.graphics.PointF
import android.os.SystemClock
import android.support.annotation.UiThread
import android.support.v4.math.MathUtils
import android.view.MotionEvent
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.mozilla.focus.ext.use
import org.mozilla.focus.utils.Direction
import java.util.EnumSet
import java.util.concurrent.TimeUnit

private const val UPDATE_DELAY_MILLIS = 20L

private const val FRICTION = 0.98f
private const val MAX_SPEED = 25f

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
        private val onUpdate: (x: Float, y: Float, scrollVel: PointF) -> Unit,
        private val simulateTouchEvent: (MotionEvent) -> Unit
) {

    private val scrollVelReturnVal = PointF()

    /**
     * The bounds inside which this cursor should remain, i.e. the screen bounds.
     * Must be set by the caller.
     */
    var maxBounds = PointF(0f, 0f)
        @UiThread set(value) {
            field = value
            if (isInitialPosSet) {
                clampPos(pos, value)
            } else {
                isInitialPosSet = true
                pos.set(value.x / 2, value.y / 2) // Center.
            }
            onUpdate(pos.x, pos.y, getScrollVel())
        }

    private var isInitialPosSet = false
    private val pos = PointF(0f, 0f)
    private var vel = 0f

    private val pressedDirections = EnumSet.noneOf(Direction::class.java)

    private var updateLoop: Deferred<Unit>? = null

    // TODO: Distances should update as a function of time to prevent lag.
    // TODO: Describe algorithm more naturally as velocity and acceleration vectors.
    private fun update(deltaMillis: Long) {
        vel = (vel + 1) * FRICTION
        vel = Math.min(MAX_SPEED, vel)
        val isMovingDiagonal = pressedDirections.size > 1
        val finalVel = if (isMovingDiagonal) vel / 2 else vel

        for (dir in pressedDirections) {
            updatePosForVel(dir, finalVel)
        }

        clampPos(pos, maxBounds)
        onUpdate(pos.x, pos.y, getScrollVel())
    }

    private fun updatePosForVel(dir: Direction, vel: Float) {
        when (dir) {
            Direction.UP -> pos.y -= vel
            Direction.DOWN -> pos.y += vel
            Direction.LEFT -> pos.x -= vel
            Direction.RIGHT -> pos.x += vel
        }
    }

    private fun getScrollVel(): PointF {
        scrollVelReturnVal.set(0f, 0f)
        if (vel > 0f) {
            if (pos.x == 0f && pressedDirections.contains(Direction.LEFT)) {
                scrollVelReturnVal.x = -vel
            } else if (pos.x == maxBounds.x && pressedDirections.contains(Direction.RIGHT)) {
                scrollVelReturnVal.x = vel
            }

            if (pos.y == 0f && pressedDirections.contains(Direction.UP)) {
                scrollVelReturnVal.y = -vel
            } else if (pos.y == maxBounds.y && pressedDirections.contains(Direction.DOWN)) {
                scrollVelReturnVal.y = vel
            }
        }
        return scrollVelReturnVal
    }

    // TODO: stop when new views (e.g. overlay) are opened.
    private fun asyncStartUpdates() = async(UI) { // Use UI to avoid synchronization.
        var currentFrameMillis = SystemClock.uptimeMillis() // duped in loop.
        var prevFrameMillis: Long
        var deltaMillis = UPDATE_DELAY_MILLIS // Move ~1 frame to start.
        while (isActive) {
            update(deltaMillis)

            delay(UPDATE_DELAY_MILLIS, TimeUnit.MILLISECONDS)

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
