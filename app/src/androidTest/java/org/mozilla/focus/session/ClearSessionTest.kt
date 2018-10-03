/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.focus.session

import android.support.test.espresso.IdlingRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.SkipOnboardingMainActivityTestRule
import org.mozilla.focus.helpers.MockServerHelper
import org.mozilla.focus.helpers.SessionLoadedIdlingResource
import org.mozilla.focus.robots.browser
import org.mozilla.focus.robots.home
import org.mozilla.focus.robots.settings

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
            assertCannotGoBack()

            assertCannotGoForward()

            navigateToPage(endpoints[0])

            openMenu()

            assertCanGoBack()

            navigateToPage(endpoints[1])

            openMenu()

            goBack()

            assertCanGoBack()

            assertCanGoForward()

            openSettings()
        }

        browser {
            addTestCookie()

            assertCookieExists()
        }

        settings {
            clearAllDataAndReturnHome()
        }

        browser {
            assertCookieDoesNotExist()
        }

        home {
            assertCannotGoBack()

            assertCannotGoForward()
        }
    }
}
