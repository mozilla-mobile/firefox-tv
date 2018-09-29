/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.robots

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.focus.R
import org.mozilla.focus.ext.click

/**
 * Implementation of Robot Pattern for the settings page.
 *
 * This has two primary benefits: 1) less brittle code, and 2) more declarative tests.
 *
 * 1) If our UI changes, we can update the robot to match and other tests should remain valid
 *
 * 2) Tests written using robots include no implementation details, only what is being tested
 *
 * See: https://academy.realm.io/posts/kau-jake-wharton-testing-robots/
 * See: https://medium.com/android-bits/espresso-robot-pattern-in-kotlin-fc820ce250f7
 */
class SettingsRobot {

    fun clearAllDataAndReturnHome() {
        clearDataButton().click()
        dialogOkButton().click()
    }

}

/**
 * Applies [func] to a new [SettingsRobot]
 *
 * @sample org.mozilla.focus.session.ClearSessionTest.WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable
 */
fun settings(func: SettingsRobot.() -> Unit) = SettingsRobot().apply(func)

private fun sendDataToggle() = onView(withId(R.id.telemetryButton))
private fun aboutButton() = onView(withId(R.id.aboutButton))
private fun privacyButton() = onView(withId(R.id.privacyNoticeButton))
private fun clearDataButton() = onView(withId(R.id.deleteButton))
private fun dialogOkButton() = onView(withId(android.R.id.button1))
private fun dialogCancelButton() = onView(withId(android.R.id.button2))
