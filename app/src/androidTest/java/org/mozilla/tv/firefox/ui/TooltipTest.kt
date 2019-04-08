/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

private const val TURBO_MODE_TEXT = "Turbo Mode"
private const val BACK_TEXT = "Navigate back"
private const val FORWARD_TEXT = "Navigate forward"
private const val RELOAD_TEXT = "Reload website"
private const val PIN_TEXT = "Pin to homescreen"
private const val DESKTOP_MODE_TEXT = "Request desktop version of this site"
private const val EXIT_TEXT = "Exit Firefox"
private const val SETTINGS_TEXT = "Settings"

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
        navigationOverlay {
            // Url bar is focused on startup and next focus up will be turbo mode button
            remoteUp()
            assertTooltipText(TURBO_MODE_TEXT)
            remoteCenter()
            assertTooltipText(TURBO_MODE_TEXT)
        }.openTileToBrowser(1) {
        }.openOverlay {
        }.openTileToBrowser(2) {
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
        }.unpinSite {
        }.openOverlay {
            // This sequence focuses the pin button from the url bar
            remoteUp()
            remoteRight(2)
            assertTooltipText(PIN_TEXT)
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
            // Focus exit button
            remoteRight()
            assertTooltipText(EXIT_TEXT)
            // Focus settings button
            remoteRight()
            assertTooltipText(SETTINGS_TEXT)
        }
    }
}
