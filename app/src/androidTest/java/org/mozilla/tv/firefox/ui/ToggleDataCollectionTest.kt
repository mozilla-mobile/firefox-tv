/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TestFunctionName")

package org.mozilla.tv.firefox.ui

import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.FirefoxTestApplication
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.settings.IS_TELEMETRY_ENABLED_DEFAULT
import org.mozilla.tv.firefox.settings.SettingsRepo
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

class ToggleDataCollectionTest {
    @get:Rule val activityTestRule = MainActivityTestRule()

    private lateinit var settingsRepo: SettingsRepo

    @Before
    fun setUp() {
        settingsRepo = (activityTestRule.activity.application as FirefoxTestApplication).settingsRepo
    }

    @Test
    fun WHEN_data_collection_button_is_toggled_THEN_data_collection_matches_button_state() {
        navigationOverlay {
        }.openSettings {
            assertDataCollectionButtonState(IS_TELEMETRY_ENABLED_DEFAULT)

            toggleDataCollectionButton()
            assertNotNull(settingsRepo.dataCollectionEnabled.value)
            assertDataCollectionButtonState(settingsRepo.dataCollectionEnabled.value ?: false)

            toggleDataCollectionButton()
            assertNotNull(settingsRepo.dataCollectionEnabled.value)
            assertDataCollectionButtonState(settingsRepo.dataCollectionEnabled.value ?: false)
        }
    }

    @Test
    fun WHEN_data_collection_button_is_toggled_THEN_button_state_persists_when_returning_to_settings() {
        var cachedDataCollectionButtonIsChecked = IS_TELEMETRY_ENABLED_DEFAULT

        navigationOverlay {
        }.openSettings {
            assertDataCollectionButtonState(cachedDataCollectionButtonIsChecked)
            toggleDataCollectionButton()
            cachedDataCollectionButtonIsChecked = !cachedDataCollectionButtonIsChecked
        }.exitToOverlay {
        }.openSettings {
            assertDataCollectionButtonState(cachedDataCollectionButtonIsChecked)
        }
    }
}
