/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.tv.firefox.ui

import android.support.test.espresso.IdlingRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.MockWebServerHelper
import org.mozilla.tv.firefox.helpers.SessionLoadedIdlingResource
import org.mozilla.tv.firefox.helpers.SkipOnboardingMainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.engineInternals
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test to verify that session data is erased when "Clear data" is pressed.
 */
class ClearDataTest {

    @Rule @JvmField
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
    fun WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable() {
        val endpoints = MockWebServerHelper
                .initMockWebServerAndReturnEndpoints("This is Google", "This is YouTube")

        navigationOverlay {
            assertCanGoBackForward(false, false)

        }.enterUrlAndEnterToBrowser(endpoints[0]) {
        }.openOverlay {
            assertCanGoBackForward(true, false)

        }.enterUrlAndEnterToBrowser(endpoints[1]) {
        }.openOverlay {
            goBack()
            assertCanGoBackForward(true, true)

            engineInternals {
                addCookie()
                assertCookieExists()
            }

        }.openSettings {
        }.clearAllDataToOverlay {
            assertCanGoBackForward(false, false)

            engineInternals {
                assertCookieDoesNotExist()
            }
        }
    }
}
