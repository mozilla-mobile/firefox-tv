/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.os.Looper

/**
 * A collection of assertion functions.
 *
 * We create our own assertion class because `assert` in Java is known to have problems: it is
 * disabled by default and must be enabled as part of the compile stage, making it easy to forget.
 */
object Assert {

    fun isUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) { return }
        throw AssertionError("UI thread was expected")
    }
}
