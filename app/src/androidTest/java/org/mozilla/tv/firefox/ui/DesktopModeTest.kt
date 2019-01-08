/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test for desktop mode feature including:
 * - Desktop mode button enabled state updates correctly
 * - Desktop mode button checked state updates correctly
 * - Desktop mode turns off when navigating to new domain
 */

/* TODO: Change to using mockWebServer, so the test does not rely on having a network connection */

class DesktopModeTest {

    @get:Rule val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun desktopModeTest() {

        val mozillaUrl = "mozilla.org"
        val googleUrl = "google.com"

        navigationOverlay {
        }.toggleTurbo {
            // Work around for #1444: on emulators with turbo mode enabled, the url bar will display
            // any url loaded by the page in addition to the primary url. We disable turbo mode to
            // ensure we only see the primary url and can assert it correctly.
        }.openOverlay {
            assertDesktopModeEnabled(false)

        }.enterUrlAndEnterToBrowser(mozillaUrl.toUri()!!) {
        }.openOverlay {
        }.turnDesktopModeOn {
        }.openOverlay {
        }.turnDesktopModeOff {
        }.openOverlay {
        }.turnDesktopModeOn {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(googleUrl.toUri()!!) {
        }.openOverlay {
        }.turnDesktopModeOn { } // Ensure desktop mode turns off when navigating to a new domain
    }
}
