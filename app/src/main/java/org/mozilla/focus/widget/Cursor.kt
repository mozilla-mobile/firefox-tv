/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.support.annotation.UiThread
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.utils.Direction
import java.util.HashSet
import java.util.concurrent.TimeUnit

/** A drawn Cursor: see [CursorViewModel] for responding to keys and setting position. */
class Cursor(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var onLayout: (width: Int, height: Int) -> Unit = { _, _ -> }

    private val CURSOR_SIZE = 45f

    private val CURSOR_ALPHA = 102
    private val CURSOR_ANIMATION_DURATION = 250
    private val CURSOR_HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)
    private val VIEW_MIN_ALPHA = 0f
    private val VIEW_MAX_ALPHA = 1f

    var cursorEvent: CursorEvent? = null
    private val paint: Paint
    private val pos = PointF()

    var speed = 0f
        private set
    private val activeDirections = HashSet<Direction>()

    private var isInit: Boolean = false
    private var moving: Boolean = false

    init {

        isInit = true
        // create the Paint and set its color
        paint = Paint()
        paint.style = Paint.Style.FILL
        paint.alpha = CURSOR_ALPHA
        paint.isAntiAlias = true
    }

    @UiThread
    fun updatePosition(x: Float, y: Float) {
        pos.set(x, y)
        invalidate()
    }

    fun stopMoving(direction: Direction) {
        activeDirections.remove(direction)

        if (activeDirections.size == 0) {
            moving = false
            speed = 0f

            animate().alpha(VIEW_MIN_ALPHA)
                    .setDuration(CURSOR_ANIMATION_DURATION.toLong()).startDelay = CURSOR_HIDE_AFTER_MILLIS
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed && isInit) {
            isInit = false
            onLayout(right, bottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.shader = RadialGradient(pos.x, pos.y, 45f, resources.getColor(R.color.teal50), resources.getColor(R.color.photonBlue50), Shader.TileMode.CLAMP)
        canvas.drawCircle(pos.x, pos.y, CURSOR_SIZE, paint)
    }

}
