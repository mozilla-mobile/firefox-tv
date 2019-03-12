/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import androidx.test.espresso.web.webdriver.Locator
import androidx.test.filters.FlakyTest

import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * Tests that the browser can successfully load pages from the network. This works with our other UI tests
 * which should load content from the disk to make them less fragile.
 *
 * This test loads a few pages off the network and verifies their content to ensure they're actually loaded.
 * Verification is minimal to reduce the chance of the test breaking from the web page changing.
 */
@FlakyTest(detail = "Requires network access")
class NetworkPageLoadTest {

    @Rule @JvmField val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun networkPageLoadTest() {
        val googleUrl = "https://google.com".toUri()!!
        val mozillaUrl = "https://mozilla.org".toUri()!!

        navigationOverlay {
        // Work around for #1444: on emulators with turbo mode enabled, the url bar will display
        // any url loaded by the page in addition to the primary url. We disable turbo mode to
        // ensure we only see the primary url and can assert it correctly.
        }.toggleTurbo {
        }.openOverlay {
            assertTurboIsSelected(false)

        }.enterUrlAndEnterToBrowser(googleUrl) {
            assertDOMElementExists(Locator.ID, "hplogo") // google logo
        }.openOverlay {
            assertURLBarTextContains("google")

        }.enterUrlAndEnterToBrowser(mozillaUrl) {
            assertDOMElementExists(Locator.CLASS_NAME, "mzp-c-navigation-logo") // mozilla logo
        }.openOverlay {
            assertURLBarTextContains("mozilla")
        }
    }
}
