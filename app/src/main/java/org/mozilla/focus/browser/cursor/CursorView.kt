/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.R

private const val CURSOR_ALPHA = 102

/** A drawn Cursor: see [CursorViewModel] for responding to keys and setting position. */
class CursorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // The radius is half the size of the container its drawn in.
    private val radius = context.resources.getDimensionPixelSize(R.dimen.remote_cursor_size) / 2f
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        alpha = CURSOR_ALPHA
        isAntiAlias = true
        shader = RadialGradient(radius, radius, /* radius */ radius,
                ContextCompat.getColor(context, R.color.teal50), ContextCompat.getColor(context, R.color.photonBlue50),
                Shader.TileMode.CLAMP)
    }

    @UiThread
    fun updatePosition(x: Float, y: Float) {
        // In onDraw, we offset the initial position from the origin: we undo that offset here.
        translationX = x - radius
        translationY = y - radius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // We draw the cursor in the top-left corner, offset so the circle isn't clipped by its
        // parent. The translationX/Y properties will draw it into the desired position on the
        // screen. We do it this way to avoid allocating new RadialGradients, which are
        // constructed in relation to the current position of the circle.
        canvas.drawCircle(radius, radius, radius, paint)
    }
}
