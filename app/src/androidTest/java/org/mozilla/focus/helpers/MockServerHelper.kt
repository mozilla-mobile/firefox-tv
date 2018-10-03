/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import android.net.Uri
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.mozilla.focus.ext.toUri

object MockServerHelper {

    fun initMockServerAndReturnEndpoints(vararg messages: String): List<Uri> {
        val mockServer = MockWebServer()
        var uniquePath = 0
        val uris = mutableListOf<Uri>()
        messages.forEach { message ->
            val response = MockResponse().setBody("<html><body>$message</body></html>")
            mockServer.enqueue(response)
            val endpoint = mockServer.url(uniquePath++.toString()).toString().toUri()!!
            uris += endpoint
        }
        return uris
    }
}
