/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

private const val TURBO_MODE_TEXT = "Turbo Mode"
private const val BACK_TEXT = "Navigate back"
private const val FORWARD_TEXT = "Navigate forward"
private const val RELOAD_TEXT = "Reload website"
private const val PIN_TEXT = "Pin to homescreen"
private const val UNPIN_TEXT = "Unpin from homescreen"
private const val DESKTOP_MODE_TEXT = "Request desktop version of this site"
private const val FXA_SIGN_IN = "New! Send Firefox tabs to Fire TV"
private const val EXIT_TEXT = "Exit Firefox"

/**
 * A test for the nav bar tooltips including:
 * - Each tooltip shows the correct string
 * - The string dynamically updates the ON/OFF state (only applicable to Turbo Mode)
 */
class TooltipTest {

    @get:Rule val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun tooltipTest() {
        val server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }

        val pages = TestAssetHelper.getGenericAssets(server)

        navigationOverlay {
            // Url bar is focused on startup and next focus up will be turbo mode button
            remoteUp()
            assertTooltipText(TURBO_MODE_TEXT)
            remoteCenter()
            assertTooltipText(TURBO_MODE_TEXT)
            remoteCenter()
        }.enterUrlAndEnterToBrowser(pages[0].url) {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(pages[1].url) {
        }.openOverlay {
            // Focus back button
            remoteUp()
            assertTooltipText(BACK_TEXT)
        }.goBack {
        }.openOverlay {
            // Focus forward button
            remoteUp()
            assertTooltipText(FORWARD_TEXT)
            // Focus reload button
            remoteRight()
            assertTooltipText(RELOAD_TEXT)
            // Focus pin button
            remoteRight()
            assertTooltipText(PIN_TEXT)
        }.pinSite {
        }.openOverlay {
            // This sequence focuses the pin button from the url bar
            remoteUp()
            remoteRight(2)
            assertTooltipText(UNPIN_TEXT)
            // Focus turbo mode button
            remoteRight()
            assertTooltipText(TURBO_MODE_TEXT)
            // Focus desktop mode button
            remoteRight()
            assertTooltipText(DESKTOP_MODE_TEXT)
        }.turnDesktopModeOn {
        }.openOverlay {
            // This sequence focuses the desktop mode button from the url bar
            remoteUp()
            remoteRight(4)
            assertTooltipText(DESKTOP_MODE_TEXT)
            // Focus FxA sign in button
            remoteRight()
            assertTooltipText(FXA_SIGN_IN)
            // Focus exit button
            remoteRight()
            assertTooltipText(EXIT_TEXT)
        }
    }
}
