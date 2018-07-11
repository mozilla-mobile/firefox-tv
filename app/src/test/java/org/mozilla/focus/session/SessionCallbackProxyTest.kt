/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import mozilla.components.browser.session.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mozilla.focus.iwebview.IWebView
import org.robolectric.RobolectricTestRunner

private const val TEST_URL = "https://github.com/mozilla-mobile/focus-android/"

@RunWith(RobolectricTestRunner::class)
class SessionCallbackProxyTest {

    @Test
    fun testOnPageStarted() {
        val session = Session("about:blank")
        val proxy = SessionCallbackProxy(session, mock(IWebView.Callback::class.java))

        proxy.onPageStarted(TEST_URL)

        assertEquals(TEST_URL, session.url)
        assertEquals(true, session.loading)
        assertEquals(false, session.securityInfo.secure)
        assertEquals(SessionCallbackProxy.MINIMUM_PROGRESS, session.progress)
    }

    @Test
    fun testOnPageFinished() {
        val session = Session("about:blank")
        val proxy = SessionCallbackProxy(session, mock(IWebView.Callback::class.java))

        proxy.onPageFinished(true)

        assertEquals(false, session.loading)
        assertEquals(true, session.securityInfo.secure)
    }

    @Test
    fun testOnProgress() {
        val session = Session("about:blank")
        val proxy = SessionCallbackProxy(session, mock(IWebView.Callback::class.java))

        proxy.onProgress(1)

        assertEquals(SessionCallbackProxy.MINIMUM_PROGRESS, session.progress)

        proxy.onProgress(42)

        assertEquals(42, session.progress)
    }

    @Test
    fun testOnUrlChanged() {
        val session = Session("about:blank")
        val proxy = SessionCallbackProxy(session, mock(IWebView.Callback::class.java))

        proxy.onURLChanged(TEST_URL)

        assertEquals(TEST_URL, session.url)
    }
}