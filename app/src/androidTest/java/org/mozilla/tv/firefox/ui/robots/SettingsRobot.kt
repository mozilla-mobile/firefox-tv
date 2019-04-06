/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import mozilla.components.support.android.test.espresso.assertHasFocus
import mozilla.components.support.android.test.espresso.assertIsChecked
import mozilla.components.support.android.test.espresso.click
import org.mozilla.tv.firefox.R

/**
 * Implementation of Robot Pattern for the settings page.
 */
class SettingsRobot {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun toggleDataCollectionButton() {
        dataCollectionButton().click()
    }

    fun assertDataCollectionButtonState(isChecked: Boolean) {
        dataCollectionButton().assertIsChecked(isChecked)
    }

    fun assertDataCollectionButtonFocused() {
        dataCollectionButton().assertHasFocus(true)
    }

    class Transition {
        fun clearAllDataToOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
            clearDataButton().click()
            // TODO: After this, the app hangs. I think it's because we restart the activity so assertions fail.

            NavigationOverlayRobot().interact()
            return NavigationOverlayRobot.Transition()
        }

        fun exitToOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
            pressBack()

            NavigationOverlayRobot().interact()
            return NavigationOverlayRobot.Transition()
        }
    }
}

private fun dataCollectionButton() = onView(withId(R.id.toggle))
private fun clearDataButton() = onView(withId(R.id.confirm_action))
