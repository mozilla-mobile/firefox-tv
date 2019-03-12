/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.ext.toUri
import java.io.IOException

object MockWebServerHelper {

    fun initMockWebServerAndReturnEndpoints(vararg messages: String): List<Uri> {
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

/**
 * A [MockWebServer] [Dispatcher] that will return Android assets in the body of requests.
 *
 * If the dispatcher is unable to read a requested asset, it will fail the test by throwing an
 * Exception on the main thread.
 *
 * @sample [org.mozilla.tv.firefox.ui.BasicNavigationTest.basicNavigationTest]
 */
class AndroidAssetDispatcher : Dispatcher() {

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun dispatch(request: RecordedRequest): MockResponse {
        val application = ApplicationProvider.getApplicationContext<FirefoxApplication>()
        val testContextPackage = "${application.packageName}.test"
        val assetManager = application
                .packageManager
                .getResourcesForApplication(testContextPackage)
                .assets
        val assetContents = try {
            val pathNoLeadingSlash = request.path.drop(1)
            assetManager.open(pathNoLeadingSlash).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) { // e.g. file not found.
            // We're on a background thread so we need to forward the exception to the main thread.
            mainThreadHandler.postAtFrontOfQueue { throw e }
            return MockResponse().setResponseCode(404)
        }

        return MockResponse().setResponseCode(200).setBody(assetContents)
    }
}
