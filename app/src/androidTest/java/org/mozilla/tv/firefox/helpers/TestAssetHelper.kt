/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.net.Uri
import okhttp3.mockwebserver.MockWebServer
import org.mozilla.tv.firefox.ext.toUri

/**
 * Helper for hosting web pages locally for testing purposes.
 */
object TestAssetHelper {

    data class TestAsset(val url: Uri, val content: String)

    // Hosts 3 simple websites, found at androidTest/assets/pages/generic[1|2|3].html
    // Returns a list of TestAsset, which can be used to navigate to each and
    // assert that the correct information is being displayed.
    fun getGenericAssets(server: MockWebServer): List<TestAsset> {
        val urls = (1..3).map { server.url("pages/generic$it.html").toString().toUri()!! }
        val contentList = (1..3).map { "Page content: $it" }

        return urls.zip(contentList) { url, content ->
            TestAsset(url, content)
        }
    }

    fun getUUIDPage(server: MockWebServer): TestAsset {
        val url = server.url("pages/basic_nav_uuid.html").toString().toUri()!!
        val content = "Page content: 3"

        return TestAsset(url, content)
    }
}
