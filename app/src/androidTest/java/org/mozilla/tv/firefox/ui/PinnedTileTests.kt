/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.MockWebServerHelper
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

class PinnedTileTests {

    @get:Rule val activityTestRule = MainActivityTestRule()

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun pinnedTileTests() {
        val endpoint = MockWebServerHelper
            .initMockWebServerAndReturnEndpoints("This is an example").first()

        navigationOverlay {
        }.enterUrlAndEnterToBrowser(endpoint) {
        }.openOverlay {
            assertPinButtonChecked(false)
        }.pinSite {
        }.openOverlay {
            assertToast(R.string.notification_pinned_site, activityTestRule)
            assertPinButtonChecked(true)
            assertPinnedTileExists(11, "localhost") // MockWebServer hosts all sites at localhost
        }.unpinSite {
        }.openOverlay {
            assertPinButtonChecked(false)
        }
    }
}
