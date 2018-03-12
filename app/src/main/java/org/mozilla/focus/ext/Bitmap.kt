/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.support.annotation.CheckResult

fun Bitmap.arePixelsAllTheSame(): Boolean {
    val testPixel = getPixel(0, 0)

    // For perf, I expect iteration order is important. Under the hood, the pixels are represented
    // by a single array: if you iterate along the buffer, you can take advantage of cache hits
    // (since several words in memory are imported each time memory is accessed).
    //
    // We choose this iteration order (width first) because getPixels writes into a single array
    // with index 1 being the same value as getPixel(1, 0) (i.e. it writes width first).
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = getPixel(x, y)
            if (color != testPixel) {
                return false
            }
        }
    }

    return true
}

@CheckResult
fun Bitmap.withRoundedCorners(cornerRadius: Float): Bitmap? {
    val newBitmapWithScreenshotDims = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(newBitmapWithScreenshotDims)
    val paint = Paint()

    // Need to set isAntiAlias to true because it smooths out the edges of what is being drawn
    paint.isAntiAlias = true

    paint.shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    canvas.drawRoundRect(RectF(0.0f, 0.0f, width.toFloat(), height.toFloat()), cornerRadius, cornerRadius, paint)
    return newBitmapWithScreenshotDims
}