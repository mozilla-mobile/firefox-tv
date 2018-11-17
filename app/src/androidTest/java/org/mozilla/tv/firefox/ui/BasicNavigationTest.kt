/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test for basic browser navigation including:
 * - Loads page content
 * - Back/forward loads the appropriate pages
 * - Reload reloads the appropriate page
 * - Back/forward/reload enabled state updates correctly
 */
class BasicNavigationTest {

    @get:Rule val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun basicNavigationTest() {
        val server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }

        val pages = TestAssetHelper.getGenericAssets(server).dropLast(1) +
            TestAssetHelper.getUUIDPage(server)

        var preReloadTitle: String? = null

        // Navigate to several URLs with the URL bar.
        navigationOverlay {
            assertCanGoBackForward(false, false)
            assertCanReload(false)
            assertURLBarDisplaysHint()

        }.enterUrlAndEnterToBrowser(pages[0].url) {
            assertTestContent(pages[0].content)
        }.openOverlay {
            assertCanGoBackForward(false, false)
            assertCanReload(true)
            assertURLBarTextContains(pages[0].url.toString())

        }.enterUrlAndEnterToBrowser(pages[1].url) {
            assertTestContent(pages[1].content)
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarTextContains(pages[1].url.toString())

        }.enterUrlAndEnterToBrowser(pages[2].url) {
            assertTestContent(pages[2].content)
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarTextContains(pages[2].url.toString())

            // Verify back behavior (to the beginning of the stack).
        }.goBack {
        }.openOverlay {
            assertCanGoBackForward(true, true)
            assertCanReload(true)
            assertURLBarTextContains(pages[1].url.toString())
        }.closeToBrowser {
            assertTestContent(pages[1].content)

        }.openOverlay {
        }.goBack {
        }.openOverlay {
            assertCanGoBackForward(false, true)
            assertCanReload(true)
            assertURLBarTextContains(pages[0].url.toString())
        }.closeToBrowser {
            assertTestContent(pages[0].content)

        }.openOverlay {
            // Verify forward behavior (to the end of the stack).
        }.goForward {
        }.openOverlay {
            assertCanGoBackForward(true, true)
            assertCanReload(true)
            assertURLBarTextContains(pages[1].url.toString())
        }.closeToBrowser {
            assertTestContent(pages[1].content)

        }.openOverlay {
        }.goForward {
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarTextContains(pages[2].url.toString())
        }.closeToBrowser {
            assertTestContent(pages[2].content)
            preReloadTitle = getPageTitle()

        }.openOverlay {
            // Verify reload behavior.
        }.reload {
        }.openOverlay {
            assertCanGoBackForward(true, false)
            assertCanReload(true)
            assertURLBarTextContains(pages[2].url.toString())
        }.closeToBrowser {
            assertTestContent(pages[2].content) // different on each reload. Use date? Verify date different?

            // We want to ensure the page is actually reloaded after reload is pressed. This
            // particular page's title changes each time the page is reloaded so we verify the
            // current title is different.
            assertNotEquals(preReloadTitle, getPageTitle())
        }
    }
}
