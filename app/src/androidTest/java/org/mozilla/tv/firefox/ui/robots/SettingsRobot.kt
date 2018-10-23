/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.ext.click

/**
 * Implementation of Robot Pattern for the settings page.
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
 * @sample org.mozilla.tv.firefox.session.ClearSessionTest.WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable
 */
fun settings(func: SettingsRobot.() -> Unit) = SettingsRobot().apply(func)

private fun sendDataToggle() = onView(withId(R.id.telemetryButton))
private fun aboutButton() = onView(withId(R.id.aboutButton))
private fun privacyButton() = onView(withId(R.id.privacyNoticeButton))
private fun clearDataButton() = onView(withId(R.id.deleteButton))
private fun dialogOkButton() = onView(withId(android.R.id.button1))
private fun dialogCancelButton() = onView(withId(android.R.id.button2))
