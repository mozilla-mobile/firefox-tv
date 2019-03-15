/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.engine

import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.webrender.CustomContentRequestInterceptor
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomContentRequestInterceptorTest {
    @Before
    fun setup() {
        SystemEngine.defaultUserAgent = "test-ua-string"
    }

    @Test
    fun `Interceptor should return content for firefox-home`() {
        val result = testInterceptor("firefox:home")

        assertNotNull(result)
        assertEquals("<html></html>", result!!.data)
        assertEquals("text/html", result.mimeType)
        assertEquals("UTF-8", result.encoding)
    }

    @Test
    fun `Interceptor should return content for firefox-about`() {
        val result = testInterceptor("firefox:about")

        assertNotNull(result)
        assertTrue(result!!.data.isNotEmpty())
        assertEquals("text/html", result.mimeType)
        assertEquals("UTF-8", result.encoding)
    }

    @Test
    fun `Interceptor should not intercept normal URLs`() {
        assertNull(testInterceptor("https://www.mozilla.org"))
        assertNull(testInterceptor("https://youtube.com/tv"))
    }

    @Test
    fun `Interceptor should return different content for firefox-home and firefox-about`() {
        val firefoxAbout = testInterceptor("firefox:about")
        val firefoxHome = testInterceptor("firefox:home")

        assertEquals(firefoxAbout!!.mimeType, firefoxHome!!.mimeType)
        assertEquals(firefoxAbout.encoding, firefoxHome.encoding)
        assertNotEquals(firefoxAbout.data, firefoxHome.data)
    }

    private fun testInterceptor(url: String): RequestInterceptor.InterceptionResponse.Content? {
        val interceptor = CustomContentRequestInterceptor(ApplicationProvider.getApplicationContext())
        return interceptor.onLoadRequest(mock(EngineSession::class.java), url)
    }
}
