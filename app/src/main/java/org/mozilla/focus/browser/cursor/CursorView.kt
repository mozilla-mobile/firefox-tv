/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.cursor

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.UiThread
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.R
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val HIDE_MESSAGE_ID = 0
private const val HIDE_ANIMATION_DURATION_MILLIS = 250L
private val HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)

/**
 * A drawn Cursor: see [CursorViewModel] for responding to keys and setting position.
 * The cursor will hide itself when it hasn't received a location update recently.
 */
class CursorView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val hideHandler = CursorHideHandler(this)

    // The radius is half the size of the container its drawn in.
    private val radius = context.resources.getDimensionPixelSize(R.dimen.remote_cursor_size) / 2f
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val cursorBg = BitmapFactory.decodeResource(resources, R.drawable.cursor_bg)
    private val cursorFg = BitmapFactory.decodeResource(resources, R.drawable.cursor_fg)

    @UiThread
    fun updatePosition(x: Float, y: Float) {
        // In onDraw, we offset the initial position from the origin: we undo that offset here.
        translationX = x - radius
        translationY = y - radius

        setMaxVisibility()
        resetCountdown()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(cursorBg, 0f, 0f, paint)
        canvas.drawBitmap(cursorFg, 0f, 0f, paint)
    }

    fun cancelUpdates() {
        animate().cancel()
        hideHandler.removeMessages(HIDE_MESSAGE_ID)
    }

    fun startUpdates() {
        setMaxVisibility()
        resetCountdown()
    }

    private fun setMaxVisibility() {
        animate().cancel()
        alpha = 1f
    }

    private fun resetCountdown() {
        hideHandler.removeMessages(HIDE_MESSAGE_ID)
        hideHandler.sendEmptyMessageDelayed(HIDE_MESSAGE_ID, HIDE_AFTER_MILLIS)
    }
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
