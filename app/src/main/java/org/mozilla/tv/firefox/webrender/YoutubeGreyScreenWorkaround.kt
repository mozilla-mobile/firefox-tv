/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.app.Activity
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Workaround fix for intermittent grey screen that shows instead of WebView when returning to
 * YouTube from the home screen, see #1865. Follow-up investigation in #1940.
 *
 * This fix sends navigation key events on the UI thread to dispel the grey screen.
 *
 * Other failed attempted workarounds:
 * - Calling engineView.resume to trigger the webview to "load" from grey screen
 * - Scrolling webview
 * - Sending non-view-changing key events
 */
object YoutubeGreyScreenWorkaround {
    fun invoke(activity: Activity?) {
        GlobalScope.launch(Dispatchers.Main) {
            delay(50)
            activity?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            activity?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
        }
    }
}
