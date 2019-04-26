/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CheckResult
import org.mozilla.tv.firefox.utils.Direction

@CheckResult(suggest = "Recycle the MotionEvent after use")
fun KeyEvent.toMotionEvent(pos: PointF): MotionEvent =
    MotionEvent.obtain(this.downTime, this.eventTime, this.action, pos.x, pos.y, 0)

fun KeyEvent.toDirection(): Direction? {
    return when (this.keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
        else -> null
    }
}
