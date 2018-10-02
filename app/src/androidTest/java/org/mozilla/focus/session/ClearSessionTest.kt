/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.focus.session

import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.SkipOnboardingMainActivityTestRule
import org.mozilla.focus.robots.browser
import org.mozilla.focus.robots.home
import org.mozilla.focus.robots.settings

@RunWith(AndroidJUnit4::class)
class ClearSessionTest {

    @Rule @JvmField
    val activityTestRule = SkipOnboardingMainActivityTestRule()

    @After
    fun tearDown() {
        activityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable() {
        home {
            assertCannotGoBack()

            assertCannotGoForward()

            openTile(1)

            openMenu()

            assertCanGoBack()

            openTile(2)

            openMenu()

            goBack()

            assertCanGoBack()

            // Forward currently takes a while to update
            // TODO uncomment this assertion when this has been fixed
            // See: https://github.com/mozilla-mobile/firefox-tv/issues/1231
            // assertCanGoForward()

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
