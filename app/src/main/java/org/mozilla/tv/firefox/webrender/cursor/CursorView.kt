/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.View
import mozilla.components.support.ktx.android.graphics.drawable.toBitmap
import org.mozilla.tv.firefox.R
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val HIDE_MESSAGE_ID = 0
private const val HIDE_ANIMATION_DURATION_MILLIS = 250L
private val HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)

/**
 * TODO
 */
class CursorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val hideHandler = CursorHideHandler(this)
    private val paint = Paint()
    private val position = PointF(x, y)

    private var cursorController: NewCursorController? = null
    private lateinit var bitmap: Bitmap
    private var width = 0f
    private var height = 0f
    private var isDisplayed = false

    fun setup(cursorController: NewCursorController) { //TODO rename
        this.cursorController = cursorController
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.width = w.toFloat()
        this.height = h.toFloat()

        val bm = context.resources.getDrawable(R.drawable.cursor_full, null).toBitmap()

        val scaleWidth = w.toFloat() / bm.width
        val scaleHeight = h.toFloat() / bm.height

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        bitmap = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, false)
        bm.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        position.x = x
        position.y = y
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        cursorController?.mutatePosition(position)
        x = position.x - (width / 2)
        y = position.y - (height / 2)

        canvas?.drawBitmap(bitmap, 0f, 0f, paint)

        if (cursorController?.shouldDisplay != isDisplayed) handleVisibility()


        invalidate()
    }

    private fun handleVisibility() {

    }

    private fun animateToInvisible() {
        animate()
                ?.setDuration(HIDE_ANIMATION_DURATION_MILLIS)
                ?.alpha(0f)
                ?.start()
    }

    private fun setMaxVisibility() {
        animate().cancel()
        alpha = 1f
    }

    /**
     * Hides the cursor when it receives a message.
     *
     * We use a [Handler], with [Message]s, because they make no allocations, unlike
     * more modern/readable approaches:
     * - coroutines
     * - Animators with start delays (and cancelling them as necessary)
     */
    private class CursorHideHandler(view: CursorView) : Handler(Looper.getMainLooper()) {
        private val viewWeakReference = WeakReference<CursorView>(view)

        override fun handleMessage(msg: Message?) {
            viewWeakReference.get()
                    ?.animate()
                    ?.setDuration(HIDE_ANIMATION_DURATION_MILLIS)
                    ?.alpha(0f)
                    ?.start()
        }
    }
}
