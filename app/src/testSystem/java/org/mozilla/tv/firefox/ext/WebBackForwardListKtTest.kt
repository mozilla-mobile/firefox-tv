/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.tv.firefox.helpers.FakeWebHistoryItem
import org.mozilla.tv.firefox.helpers.toFakeWebBackForwardList

class WebBackForwardListKtTest {

    @Test
    fun `WHEN a WebBackForwardList is converted to a List THEN the original URLs are all in order and unmodified`() {
        val expected = listOf(
            "https://google.com",
            "https://mozilla.org",
            "https://facebook.com"
        )

        // This test relies on our MockWeb* impls having no bugs but we can't do any better.
        val webBackForwardList = expected.map { FakeWebHistoryItem(mockOriginalUrl = it) }
            .toFakeWebBackForwardList()

        val actual = webBackForwardList.toList()
            .map { it.originalUrl }
        assertEquals(expected, actual)
    }
}
