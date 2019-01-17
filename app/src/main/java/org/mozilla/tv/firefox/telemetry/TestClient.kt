/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.util.Log
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Headers
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import java.io.ByteArrayInputStream

/**
 * [TestClient] for [TelemetryClient] to intercept fetch() and send request JSON payload to Log.d()
 */
class TestClient : Client() {
    override fun fetch(request: Request): Response {
        var requestBodyString = request.url
        request.body?.useStream { it.bufferedReader().use { requestBodyString += it.readText() } }
        Log.d("TELEMETRY_PAYLOAD", requestBodyString)

        // Create bogus response fields
        val headers: Headers = MutableHeaders("" to "")
        val data = "".toByteArray()
        val input = ByteArrayInputStream(data)

        return Response("DUMMY", 200, headers, Response.Body(input))
    }
}
