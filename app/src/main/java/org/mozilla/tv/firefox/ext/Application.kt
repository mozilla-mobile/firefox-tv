/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.app.Application
import android.content.Intent
import org.mozilla.tv.firefox.MainActivity

fun Application.bringAppToForeground() {
    val intent = Intent(this, MainActivity::class.java).apply {
        // TODO verify these are the correct flags
        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
