/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import android.support.test.espresso.web.webdriver.Locator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.evalJS
import org.mozilla.tv.firefox.helpers.SessionLoadedIdlingResource
import org.mozilla.tv.firefox.helpers.SkipOnboardingMainActivityTestRule
import org.mozilla.tv.firefox.webrender.WebRenderFragment

/** An integration test to verify [IWebView.executeJS] works correctly. */
class IWebViewExecuteJavascriptTest {

    @Rule @JvmField
    val activityTestRule = SkipOnboardingMainActivityTestRule()

    private lateinit var loadingIdlingResource: SessionLoadedIdlingResource
    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        loadingIdlingResource = SessionLoadedIdlingResource()
        IdlingRegistry.getInstance().register(loadingIdlingResource)

        mockServer = MockWebServer()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource)
        activityTestRule.getActivity().finishAndRemoveTask()
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
        val webView = browserFragment.webView!!
        activityTestRule.runOnUiThread {
            webView.evalJS(
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
