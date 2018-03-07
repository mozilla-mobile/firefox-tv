/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.focus.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet

/*
 * Code taken from https://stackoverflow.com/questions/20743859/imageview-rounded-corners/23713740#23713740
 *
 * */
class RoundedImageView(context: Context, attributes: AttributeSet) : AppCompatImageView(context, attributes) {
    private val mCornerRadius = 6.0f
    private var rectF: RectF? = null
    private val porterDuffXferMode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    override fun onDraw(canvas: Canvas) {
        val myDrawable = drawable

        if (myDrawable != null && myDrawable is BitmapDrawable && mCornerRadius > 0) {
            val paint = myDrawable.paint
            val color = -0x1000000
            if (rectF == null) {
                rectF = RectF(drawable.bounds)
            } else {
                rectF?.set(drawable.bounds)
            }

            // Create an off-screen bitmap to the PorterDuff alpha blending to work right
            val saveCount = canvas.saveLayer(rectF, null)
            // Resize the rounded rect we'll clip by this view's current bounds
            // (super.onDraw() will do something similar with the drawable to draw)
            imageMatrix.mapRect(rectF)

            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawRoundRect(rectF, mCornerRadius, mCornerRadius, paint)

            val oldMode = paint.xfermode
            // This is the paint already associated with the BitmapDrawable that super draws
            paint.xfermode = porterDuffXferMode
            super.onDraw(canvas)
            paint.xfermode = oldMode
            canvas.restoreToCount(saveCount)
        } else {
            super.onDraw(canvas)
        }
    }
}