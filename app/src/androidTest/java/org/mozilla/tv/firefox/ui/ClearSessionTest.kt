/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.tv.firefox.ui

import android.support.test.espresso.IdlingRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.MockServerHelper
import org.mozilla.tv.firefox.helpers.SessionLoadedIdlingResource
import org.mozilla.tv.firefox.helpers.SkipOnboardingMainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.engineInternals
import org.mozilla.tv.firefox.ui.robots.home

/**
 * A test to verify that session data is erased when "Clear data" is pressed.
 */
@RunWith(AndroidJUnit4::class)
class ClearSessionTest {

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

    @Test
    fun WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable() {
        val endpoints = MockServerHelper
                .initMockServerAndReturnEndpoints("This is Google", "This is YouTube")

        home {
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
