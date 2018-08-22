/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

/**
 * An object assigned a LiveData instance to send a stateless event. See
 * [org.mozilla.focus.utils.UserClearDataEvent] for example usage.
 *
 * This implementation is inspired by:
 *   https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
 */

class LiveDataEvent {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): LiveDataEvent? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            this
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): LiveDataEvent = this
}
