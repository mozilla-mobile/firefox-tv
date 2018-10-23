/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.view.KeyEvent

/** Keys the user can press on the remote. */
enum class RemoteKey {
    UP, DOWN, LEFT, RIGHT,
    CENTER;

    fun toDirection() = when (this) {
        UP -> Direction.UP
        DOWN -> Direction.DOWN
        LEFT -> Direction.LEFT
        RIGHT -> Direction.RIGHT
        else -> null
    }

    companion object {
        fun fromKeyEvent(event: KeyEvent) = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> UP
            KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT

            KeyEvent.KEYCODE_DPAD_CENTER -> CENTER
            else -> null
        }
    }
}
