/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.arch.lifecycle.Observer
import org.mozilla.focus.iwebview.IWebView

/** An observer for when the user clears their browsing data. See [UserClearDataEvent] for details. */
class UserClearDataEventObserver(private val webView: IWebView?) : Observer<LiveDataEvent> {
    override fun onChanged(event: LiveDataEvent?) {
        event?.getContentIfNotHandled()?.let {
            webView?.cleanup()
        }
    }
}
