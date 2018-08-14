/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.cursor

import android.support.annotation.UiThread
import android.view.KeyEvent
import org.mozilla.focus.utils.Direction
import org.mozilla.focus.utils.RemoteKey

/** Converts raw key events into high-level commands the view model can recognize. */
class CursorKeyDispatcher(
    var isEnabled: Boolean,
    private val onDirectionKey: (dir: Direction, action: Int) -> Unit,
    private val onSelectKey: (event: KeyEvent) -> Unit
) {

    /**
     * Converts key events into Cursor actions; an analog to [Activity.dispatchKeyEvent].
     *
     * @return true if this key event was handled, false otherwise.
     */
    @UiThread
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled) return false
        if (event.action != KeyEvent.ACTION_DOWN &&
                event.action != KeyEvent.ACTION_UP) return false

        val remoteKey = RemoteKey.fromKeyEvent(event)
        if (remoteKey == RemoteKey.CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER) { // For keyboard and emulator use.
            onSelectKey(event)
            return true
        }

        val direction = remoteKey?.toDirection()
        if (direction != null) {
            onDirectionKey(direction, event.action)
            return true
        }

        return false
    }
}
