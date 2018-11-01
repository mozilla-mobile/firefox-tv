/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import android.support.test.espresso.IdlingRegistry
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.SessionLoadedIdlingResource
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

private val BODY_TEXT = (1..3).map {
    "Page content: $it" // Hard-coded in the HTML files.
}

/**
 * A test for basic browser navigation including:
 * - Loads page content
 * - Back/forward loads the appropriate pages
 * - Reload reloads the appropriate page
 * - Back/forward/reload enabled state updates correctly
 */
class BasicNavigationTest {

    @Rule
    @JvmField
    val activityTestRule = MainActivityTestRule()

    private lateinit var loadingIdlingResource: SessionLoadedIdlingResource

    @Before
    fun setup() {
        loadingIdlingResource = SessionLoadedIdlingResource()
        IdlingRegistry.getInstance().register(loadingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource)
        activityTestRule.activity.finishAndRemoveTask()
    }

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun basicNavigationTest() {
        val server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }

        val pageUrlStrs = (1..3).map { i ->
            server.url("pages/basic_nav$i.html").toString()
        }
        val pageUrls = pageUrlStrs.map { it.toUri()!! }

        var preReloadTitle: String? = null

        // Navigate to several URLs with the URL bar.
        navigationOverlay {
            assertCanGoBackForward(false, false)
            assertCanReload(false)
            assertURLBarDisplaysHint()

        }.enterUrlAndEnterToBrowser(pageUrls[0]) {
            assertTestContent(BODY_TEXT[0])
        }.openOverlay {
            assertCanGoBackForward(true, false) // TODO: Unexpected. Fix in #1347.
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[0])

        }.enterUrlAndEnterToBrowser(pageUrls[1]) {
            assertTestContent(BODY_TEXT[1])
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[1])

        }.enterUrlAndEnterToBrowser(pageUrls[2]) {
            assertTestContent(BODY_TEXT[2])
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[2])

            // Verify back behavior (to the beginning of the stack).
            goBack()
            assertCanGoBackForward(true, true)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[1])
        }.closeToBrowser {
            assertTestContent(BODY_TEXT[1])

        }.openOverlay {
            goBack()
            assertCanGoBackForward(true, true) // TODO: unexpected. Fix in #1347.
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[0])
        }.closeToBrowser {
            assertTestContent(BODY_TEXT[0])

        }.openOverlay {
            // Verify forward behavior (to the end of the stack).
            goForward()
            assertCanGoBackForward(true, true)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[1])
        }.closeToBrowser {
            assertTestContent(BODY_TEXT[1])

        }.openOverlay {
            goForward()
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[2])
        }.closeToBrowser {
            assertTestContent(BODY_TEXT[2])
            preReloadTitle = getPageTitle()

        }.openOverlay {
            // Verify reload behavior.
            reload()
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarText(pageUrlStrs[2])
        }.closeToBrowser {
            assertTestContent(BODY_TEXT[2]) // different on each reload. Use date? Verify date different?

            // We want to ensure the page is actually reloaded after reload is pressed. This
            // particular page's title changes each time the page is reloaded so we verify the
            // current title is different.
            assertNotEquals(preReloadTitle, getPageTitle())
        }
    }
}
