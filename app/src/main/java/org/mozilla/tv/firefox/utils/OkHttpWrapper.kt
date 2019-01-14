/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import okhttp3.OkHttpClient
import java.io.IOException

/** A holder for the shared OkHttpClient. */
object OkHttpWrapper {
    val client = OkHttpClient.Builder().build()

    @JvmStatic
    fun onLowMemory() {
        try {
            client.cache()?.evictAll()
        } catch (_: IOException) { /* We don't care. */ }
    }
}
