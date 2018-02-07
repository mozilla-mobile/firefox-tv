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

private const val CURSOR_SIZE = 45f
private const val CURSOR_ALPHA = 102

private const val CURSOR_ANIMATION_DURATION = 250
private val CURSOR_HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)
private const val VIEW_MIN_ALPHA = 0f


/** A drawn Cursor: see [CursorViewModel] for responding to keys and setting position. */
class CursorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /** A callback when layout for this view occurs. */
    var onLayoutChanged: (width: Int, height: Int) -> Unit = { _, _ -> }

    private val pos = PointF()

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        alpha = CURSOR_ALPHA
        isAntiAlias = true
    }

    @UiThread
    fun updatePosition(x: Float, y: Float) {
        pos.set(x, y)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.shader = RadialGradient(pos.x, pos.y, 45f, resources.getColor(R.color.teal50), resources.getColor(R.color.photonBlue50), Shader.TileMode.CLAMP)
        canvas.drawCircle(pos.x, pos.y, CURSOR_SIZE, paint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            onLayoutChanged(right, bottom)
        }
    }
}
