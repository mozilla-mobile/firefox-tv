/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.mozilla.focus.R

class BrowserNavigationOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle) {

    enum class NavigationEvent {
        HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD
    }

    interface NavigationEventHandler {
        fun onEvent(event: NavigationEvent, value: String? = null)
    }

    var eventHandler: NavigationEventHandler? = null

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)
    }

    fun setNavigationEventHandler(handler: NavigationEventHandler) {
        eventHandler = handler
    }

}
