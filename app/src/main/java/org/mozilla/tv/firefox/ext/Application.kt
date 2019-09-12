/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.app.Application
import android.content.Intent
import org.mozilla.tv.firefox.MainActivity

/**
 * Creates a new intent and starts MainActivity. Note that this can be used to bring the app
 * to the foreground when it is in the background
 */
fun Application.startMainActivity() {
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
}
