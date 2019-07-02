/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.tv.firefox.ui

import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ui.robots.navigationOverlay
import org.mozilla.tv.firefox.utils.TurboMode

class ToggleTurboModeTest {
    @get:Rule val activityTestRule = MainActivityTestRule()

    private lateinit var turboMode: TurboMode

    @Before
    fun setUp() {
        turboMode = activityTestRule.activity.application.serviceLocator.turboMode
    }

    @Test
    fun WHEN_turbo_mode_button_is_toggled_THEN_turbo_mode_matches_button_state() {
        navigationOverlay {
        }.linearNavigateToTurboModeTileAndOpen {
            assertToggleButtonState(turboMode.isEnabled)

            toggleSettingsPageButton()
            assertNotNull(turboMode.isEnabled)
            assertToggleButtonState(turboMode.isEnabled)

            toggleSettingsPageButton()
            assertNotNull(turboMode.isEnabled)
            assertToggleButtonState(turboMode.isEnabled)
        }
    }

    @Test
    fun WHEN_turbo_mode_button_is_toggled_THEN_button_state_persists_when_returning_to_settings() {
        var cachedTurboModeButtonIsChecked = turboMode.isEnabled

        navigationOverlay {
        }.linearNavigateToTurboModeTileAndOpen {
            assertToggleButtonState(cachedTurboModeButtonIsChecked)
            toggleSettingsPageButton()
            cachedTurboModeButtonIsChecked = !cachedTurboModeButtonIsChecked
        }.exitToOverlay {
            linearNavigateToSettings()
        }.openSettingsTurboModeTile {
            assertToggleButtonState(cachedTurboModeButtonIsChecked)
        }
    }
}
