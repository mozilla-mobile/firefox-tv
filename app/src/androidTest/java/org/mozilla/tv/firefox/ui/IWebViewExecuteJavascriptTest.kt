/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.evalJS
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.webrender.WebRenderFragment

/** An integration test to verify [IWebView.executeJS] works correctly. */
class IWebViewExecuteJavascriptTest {

    @get:Rule val activityTestRule = MainActivityTestRule()
    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        mockServer = MockWebServer()
    }

    @Test
    fun executeJSTest() {
        val expectedLoadedText = "Loaded"
        mockServer.enqueue(MockResponse().setBody("<html><body>$expectedLoadedText</body></html>"))
        mockServer.start()
        val url = mockServer.url("").toString()

        // Load the mock page.
        onView(withId(R.id.navUrlInput))
                .perform(typeText(url), pressImeActionButton())

        // Assert loaded.
        assertBodyText(expectedLoadedText)

        // Inject JS.
        val expectedChangedText = "Changed"
        val browserFragment = activityTestRule.activity.supportFragmentManager.findFragmentByTag(
                WebRenderFragment.FRAGMENT_TAG) as WebRenderFragment
        val engineView = browserFragment.engineView!!
        activityTestRule.runOnUiThread {
            engineView.evalJS(
                    "document.getElementsByTagName('body')[0].innerText = '$expectedChangedText';")
        }

        // Assert JS was injected.
        assertBodyText(expectedChangedText)
    }

    private fun assertBodyText(expected: String) {
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "body"))
                .check(webMatches(getText(), equalTo(expected)))
    }
}
