/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.graphics.PointF
import android.os.SystemClock
import android.support.annotation.UiThread
import android.support.v4.math.MathUtils
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.mozilla.focus.ext.use
import org.mozilla.focus.utils.Direction
import org.mozilla.focus.utils.RemoteKey
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
 * - Input: convert key presses to movement events
 * - Loop: manage an event loop
 * - Update: updates & clamps (to argument bounds) the Cursor data from the event loop
 * - Notify: tell listeners about data updates, including scroll events
 *
 * When using this class, be sure to update the public properties, e.g. [onUpdate] and [maxBounds].
 *
 * We could further modularize this class by splitting out its responsibilities.
 */
class CursorViewModel(
        private val simulateTouchEvent: (MotionEvent) -> Unit
) {
    /**
     * Called when the cursor position is updated: this should be connected to the View.
     *
     * This will always be called from the UIThread.
     */
    var onUpdate: (x: Float, y: Float) -> Unit = { _, _ -> }
        @UiThread set

    /** The bounds inside which this cursor should remain, i.e. the screen bounds. */
    var maxBounds = PointF(0f, 0f)
        @UiThread set(value) {
            field = value
            clampPos(pos, value)
        }

    private val pos = PointF(0f, 0f)
    private var vel = 0f

    private val pressedDirections = mutableSetOf<Direction>()

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
        onUpdate(pos.x, pos.y)
    }

    private fun updatePosForVel(dir: Direction, vel: Float) {
        when (dir) {
            Direction.UP -> pos.y -= vel
            Direction.DOWN -> pos.y += vel
            Direction.LEFT -> pos.x -= vel
            Direction.RIGHT -> pos.x += vel
        }
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

    /**
     * Converts key events into Cursor actions; an analog to [Activity.dispatchKeyEvent].
     *
     * @return true if this key event was handled, false otherwise.
     */
    @UiThread
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN
                && event.action != KeyEvent.ACTION_UP) return false

        val remoteKey = RemoteKey.fromKeyEvent(event) ?: return false
        if (remoteKey == RemoteKey.CENTER) {
            dispatchTouchEventOnCurrentPosition(event.action)
            return true
        }

        val direction = remoteKey.toDirection()
        if (direction != null) {
            onDirectionKey(direction, event.action)
            return true
        }
        return false
    }

    private fun onDirectionKey(dir: Direction, action: Int) {
        if (action == KeyEvent.ACTION_DOWN) {
            pressedDirections.add(dir)
            if (updateLoop == null) updateLoop = asyncStartUpdates()
        } else if (action == KeyEvent.ACTION_UP) {
            pressedDirections.remove(dir)
            if (pressedDirections.isEmpty()) {
                updateLoop?.cancel()
                updateLoop = null
                vel = 0f // Stop moving.
            }
        }
    }

    /** Dispatches a touch event on the current position, sending a click where the cursor is. */
    private fun dispatchTouchEventOnCurrentPosition(action: Int) {
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now - DOWN_TIME_OFFSET_MILLIS, now, action, pos.x, pos.y, 0).use {
            simulateTouchEvent(it)
        }
    }
}

private fun clampPos(pos: PointF, maxBounds: PointF) {
    pos.x = MathUtils.clamp(pos.x, 0f, maxBounds.x)
    pos.y = MathUtils.clamp(pos.y, 0f, maxBounds.y)
}
