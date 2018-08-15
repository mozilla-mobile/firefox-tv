/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.arch.lifecycle.Observer
import android.support.v7.app.AppCompatActivity
import org.mozilla.focus.ScreenController
import java.util.*

/** An observer for when the user clears their browsing data. See [UserClearDataEvent] for details. */
class UserClearDataEventObserver(private val activity: AppCompatActivity) : Observer<LiveDataEvent> {
    override fun onChanged(event: LiveDataEvent?) {
    }
}