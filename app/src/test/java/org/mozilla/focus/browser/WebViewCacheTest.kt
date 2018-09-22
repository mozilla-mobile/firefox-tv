package org.mozilla.focus.browser

import android.util.AttributeSet
import android.widget.FrameLayout
import mozilla.components.browser.engine.system.SystemEngineView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WebViewCacheTest {

    @Test
    fun `WHEN getWebView is called multiple times THEN the same WebView is returned`() {
        val cachRefs = List(5) { getWebView() }
        cachRefs.forEach { assertEquals(cachRefs.first(), it) }
    }

    @Test
    fun `GIVEN cached WebView has a parent WHEN WebView is returned THEN WebView should be removed from parent`() {
        for (i in 0..5) {
            val webView = getWebView()
            assertEquals(null, webView.parent)
            val parent = FrameLayout(RuntimeEnvironment.application)
            parent.addView(webView)
            assertEquals(parent, webView.parent)
        }
    }

    @Test
    fun `GIVEN cache has been cleared WHEN WebView is retrieved from cache THEN it should be a different instance`() {
        val webView = getWebView()
        assertEquals(webView, getWebView())

        WebViewCache.clear()

        assertNotEquals(webView, getWebView())
    }

    private fun getWebView(): SystemEngineView =
            WebViewCache.getWebView(RuntimeEnvironment.application, mock(AttributeSet::class.java))
}