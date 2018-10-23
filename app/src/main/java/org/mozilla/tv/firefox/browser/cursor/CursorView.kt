/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.browser.cursor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.UiThread
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.KeyEvent
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.RemoteKey
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val HIDE_MESSAGE_ID = 0
private const val HIDE_ANIMATION_DURATION_MILLIS = 250L
private val HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)

/**
 * A drawn Cursor: see [CursorViewModel] for responding to keys and setting position.
 * The cursor will hide itself when it hasn't received a location update recently.
 */
class CursorView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private val hideHandler = CursorHideHandler(this)

    @UiThread
    fun updatePosition(x: Float, y: Float) {
        // translation* sets the top-left corner so we offset it in order to center the asset.
        translationX = x - width / 2
        translationY = y - height / 2

        setMaxVisibility()
        resetCountdown()
    }

    fun updateCursorPressedState(event: KeyEvent) {
        // Enter for keyboard and emulator use.
        val remoteKey = RemoteKey.fromKeyEvent(event)
        if (remoteKey == RemoteKey.CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                setImageResource(R.drawable.cursor_full_active)
            } else if (event.action == KeyEvent.ACTION_UP) {
                setImageResource(R.drawable.cursor_full)
            }
        }
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
