/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class KeyEventKtTest {

    @Test
    fun `WHEN key event is center or enter THEN keyCode is select`() {
        arrayOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER
        ).forEach {
            val keyEvent = KeyEvent(ACTION_DOWN, it)
            assertTrue(keyEvent.toString(), keyEvent.isKeyCodeSelect)
        }
    }

    @Test
    fun `WHEN key event is not a select event THEN keyCode is not select`() {
        // Choose non-select keyEvents arbitrarily: as an Int, we could make a more thorough test but it's not worth it.
        arrayOf(
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME
        ).forEach {
            val keyEvent = KeyEvent(ACTION_DOWN, it)
            assertFalse(keyEvent.toString(), keyEvent.isKeyCodeSelect)
        }
    }
}
