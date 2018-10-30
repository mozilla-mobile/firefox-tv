/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import android.support.test.espresso.IdlingRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.SessionLoadedIdlingResource
import org.mozilla.tv.firefox.helpers.SkipOnboardingMainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.browser
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test for basic browser navigation including:
 * - Loads page content
 * - Back/forward loads the appropriate pages
 * - Reload reloads the appropriate page
 * - Back/forward/reload enabled state updates correctly
 */
@RunWith(AndroidJUnit4::class)
class DesktopModeTest {
    @Rule
    @JvmField
    val activityTestRule = SkipOnboardingMainActivityTestRule()

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
    fun desktopModeTest() {

        val mozilla_url = "mozilla.org"
        val google_url = "google.com"

        navigationOverlay {
            assertDesktopModeEnabled(false)

        }.enterUrlAndEnterToBrowser(mozilla_url.toUri()!!) {
        }.openOverlay {
            assertCanTurnDesktopModeOn(true)
            // Turn desktop mode on
            toggleDesktopMode()
        }
        browser {
        }.openOverlay {
            assertCanTurnDesktopModeOn(false)
            // Turn desktop mode off
            toggleDesktopMode()
        }
        browser {
        }.openOverlay {
            assertCanTurnDesktopModeOn(true)

            // Turn desktop mode on
            toggleDesktopMode()
        }
        browser {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(google_url.toUri()!!) {
        }.openOverlay {
            // Ensure desktop mode turns off when navigating to a new domain
            assertCanTurnDesktopModeOn(true)
        }
    }
}
