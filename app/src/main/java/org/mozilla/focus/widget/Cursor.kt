/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.util.AttributeSet
import android.view.View

import org.mozilla.focus.R
import org.mozilla.focus.utils.Direction
import org.mozilla.focus.utils.Edge

import java.util.HashSet
import java.util.concurrent.TimeUnit

class Cursor(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val CURSOR_SIZE = 45f

    private val MAX_SPEED = 25
    private val FRICTION = 0.98
    private val CURSOR_ALPHA = 102
    private val CURSOR_ANIMATION_DURATION = 250
    private val CURSOR_HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)
    private val VIEW_MIN_ALPHA = 0f
    private val VIEW_MAX_ALPHA = 1f

    var cursorEvent: CursorEvent? = null
    private val paint: Paint
    private var x: Int = 0
    private var y: Int = 0
    var speed = 0f
        private set
    private val activeDirections = HashSet<Direction>()
    private var maxHeight: Int = 0
    private var maxWidth: Int = 0

    private var isInit: Boolean = false
    private var moving: Boolean = false

    // Make sure we run the update on the main thread
    private val handler = Handler()
    private val tick = object : Runnable {
        override fun run() {
            move()
            handler.postDelayed(this, 20)
        }
    }

    val location: Point
        get() = Point(x, y)

    init {

        isInit = true
        // create the Paint and set its color
        paint = Paint()
        paint.style = Paint.Style.FILL
        paint.alpha = CURSOR_ALPHA
        paint.isAntiAlias = true
    }

    fun moveCursor(direction: Direction) {
        activeDirections.add(direction)

        // If the cursor isn't moving start the move loop
        if (!moving) {
            animate().cancel()
            alpha = VIEW_MAX_ALPHA

            moving = true
            handler.post(tick)
        }
    }

    fun stopMoving(direction: Direction) {
        activeDirections.remove(direction)

        if (activeDirections.size == 0) {
            handler.removeCallbacks(tick)
            moving = false
            speed = 0f

            animate().alpha(VIEW_MIN_ALPHA)
                    .setDuration(CURSOR_ANIMATION_DURATION.toLong()).startDelay = CURSOR_HIDE_AFTER_MILLIS
        }
    }

    private fun move() {
        speed++
        speed *= FRICTION.toFloat()
        speed = Math.min(MAX_SPEED.toFloat(), speed)
        val isMovingDiagonal = activeDirections.size > 1
        val moveSpeed = if (isMovingDiagonal) speed / 2 else speed

        for (direction in activeDirections) {
            moveOneDirection(direction, Math.round(moveSpeed))
        }

        invalidate()
    }

    private fun moveOneDirection(direction: Direction, amount: Int) {
        when (direction) {
            Direction.DOWN -> {
                if (y >= maxHeight - CURSOR_SIZE) {
                    cursorEvent!!.cursorHitEdge(Edge.BOTTOM)
                    return
                }

                y = y + amount
            }
            Direction.LEFT -> {
                if (x <= 0 + CURSOR_SIZE) {
                    cursorEvent!!.cursorHitEdge(Edge.LEFT)
                    return
                }

                x = x - amount
            }
            Direction.RIGHT -> {
                if (x >= maxWidth - CURSOR_SIZE) {
                    cursorEvent!!.cursorHitEdge(Edge.RIGHT)
                    return
                }
                x = x + amount
            }
            Direction.UP -> {
                if (y <= 0 + CURSOR_SIZE) {
                    cursorEvent!!.cursorHitEdge(Edge.TOP)
                    return
                }

                y = y - amount
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (isInit) {
            maxHeight = height
            maxWidth = width
            x = maxWidth / 2
            y = maxHeight / 2
            isInit = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.shader = RadialGradient(x.toFloat(), y.toFloat(), 45f, resources.getColor(R.color.teal50), resources.getColor(R.color.photonBlue50), Shader.TileMode.CLAMP)
        canvas.drawCircle(x.toFloat(), y.toFloat(), CURSOR_SIZE, paint)
    }

}
