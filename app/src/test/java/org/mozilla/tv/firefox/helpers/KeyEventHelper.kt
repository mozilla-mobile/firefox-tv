/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.view.KeyEvent

private val RANDOM_KEYCODES = setOf(
    KeyEvent.KEYCODE_HOME,
    KeyEvent.KEYCODE_MENU,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_B
)

/**
 * Helpers functions for manipulating KeyEvents.
 */
object KeyEventHelper {

    fun getDownUpKeyEvents(keyCodes: Collection<Int>): List<KeyEvent> = keyCodes.flatMap { keyCode ->
        arrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).map { action -> KeyEvent(action, keyCode) }
    }

    fun getDownUpKeyEvents(keyCode: Int): List<KeyEvent> = getDownUpKeyEvents(listOf(keyCode))

    fun getRandomKeyEventsExcept(excludedKeyCodes: Collection<Int>): List<KeyEvent> {
        val returnedKeyCodes = RANDOM_KEYCODES - excludedKeyCodes
        return getDownUpKeyEvents(returnedKeyCodes)
    }

    fun getRandomKeyEventsExcept(excludedKeyCode: Int): List<KeyEvent> = getRandomKeyEventsExcept(listOf(excludedKeyCode))
}
