package org.mozilla.focus.browser

import android.os.Build
import android.webkit.WebSettings

import com.amazon.android.webkit.AmazonWebSettings

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.web.WebViewProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {

    @Test
    fun testGetUABrowserString() {
        // Typical situation with a webview UA string from Android 5:
        var focusToken = "Focus/1.0"
        val existing = "Mozilla/5.0 (Linux; Android 5.0.2; Android SDK built for x86_64 Build/LSY66K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36"
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 $focusToken Chrome/37.0.0.0 Mobile Safari/537.36",
                UserAgent.getUABrowserString(existing, focusToken))

        // Make sure we can use any token, e.g Klar:
        focusToken = "Klar/2.0"
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 $focusToken Chrome/37.0.0.0 Mobile Safari/537.36",
                UserAgent.getUABrowserString(existing, focusToken))

        // And a non-standard UA String, which doesn't contain AppleWebKit
        focusToken = "Focus/1.0"
        val imaginaryKit = "Mozilla/5.0 (Linux) ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36"
        assertEquals("ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 $focusToken Chrome/37.0.0.0 Mobile Safari/537.36",
                UserAgent.getUABrowserString(imaginaryKit, focusToken))

        // Another non-standard UA String, this time with no Chrome (in which case we should be appending focus)
        val chromeless = "Mozilla/5.0 (Linux; Android 5.0.2; Android SDK built for x86_64 Build/LSY66K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36"
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36 " + focusToken,
                UserAgent.getUABrowserString(chromeless, focusToken))

        // No AppleWebkit, no Chrome
        val chromelessImaginaryKit = "Mozilla/5.0 (Linux) ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36"
        assertEquals("ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36 " + focusToken,
                UserAgent.getUABrowserString(chromelessImaginaryKit, focusToken))

    }

    @Test
    fun buildUserAgentString() {
        // It's actually possible to get a normal webview instance with real settings and user
        // agent string, which buildUserAgentString() can successfully operate on. However we can't
        // easily test that the output is expected (without simply replicating what buildUserAgentString does),
        // so instead we just use mocking to supply a fixed UA string - we then know exactly what
        // the output String should look like:
        val testSettings = mock(AmazonWebSettings::class.java)
        `when`(testSettings.userAgentString).thenReturn("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30")

        assertEquals("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ") AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 fakeappname/null",
                UserAgent.buildUserAgentString(RuntimeEnvironment.application, testSettings, "fakeappname"))
    }
}