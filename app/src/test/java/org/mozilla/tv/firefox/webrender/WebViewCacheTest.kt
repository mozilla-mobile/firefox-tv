/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.util.AttributeSet
import android.widget.FrameLayout
import mozilla.components.browser.engine.system.SystemEngineView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WebViewCacheTest {

    private lateinit var webViewCache: WebViewCache

    @Before
    fun setup() {
        webViewCache = WebViewCache()
    }

    @Test
    fun `WHEN getWebView is called multiple times THEN the same WebView is returned`() {
        val webView = getWebView()
        val message = "Expected saved webView to equal retrieved WebView"
        assertTrue(message, webView === getWebView())
        assertTrue(message, webView === getWebView())
        assertTrue(message, webView === getWebView())
    }

    @Test
    fun `GIVEN cached WebView has a parent WHEN WebView is returned THEN WebView should be removed from parent`() {
        // Setup
        val webView = getWebView()
        assertEquals(null, webView.parent)
        val parent = FrameLayout(RuntimeEnvironment.application)
        parent.addView(webView)
        assertEquals(parent, webView.parent)

        // Test
        assertEquals(null, getWebView().parent)
    }

    private fun getWebView(): SystemEngineView =
            webViewCache.getWebView(RuntimeEnvironment.application, mock(AttributeSet::class.java)) {}
}