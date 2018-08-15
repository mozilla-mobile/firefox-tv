/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

/**
 * An event for when the user clears their browsing data. To listen for the event, observe
 * on [liveData]: a new instance will be set each time the event occurs.
 *
 * This class wraps the file-private mutable LiveData instance in a read-only interface exposed
 * outside of this file.
 */
object UserClearDataEvent {
    val liveData: LiveData<LiveDataEvent> = mutableClearEventLiveData

    // Wrap the LiveData assignment in a function to explain what it does.
    fun sendUserClearDataEvent() { mutableClearEventLiveData.value = LiveDataEvent() }
}
private val mutableClearEventLiveData = MutableLiveData<LiveDataEvent>()
