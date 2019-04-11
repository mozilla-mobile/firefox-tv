/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots

import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule

import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

class SettingsTest : ScreenshotTest() {
    @get:Rule val activityTestRule = MainActivityTestRule()

    @After
    fun tearDown() {
        activityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun showSettingsViews() {
        navigationOverlay {
            onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()))
            onView(allOf(withId(R.id.container_web_render), isDisplayed()))
            linearNavigateToSettings()

            // capture a screenshot of the default settings list
            Screengrab.screenshot("settings")
        }.openSettingsTelemetryTile {
        }.exitToOverlay {
            // capture a screenshot of the Telemetry settings screen
            Screengrab.screenshot("send-usage-data")
        }.openSettingsCleardataTile {
            // capture a screenshot of the Clear Data settings screen
            Screengrab.screenshot("clear-all-data")
        }.exitToOverlay {
        }.openSettingsAboutTile {
            Screengrab.screenshot("about-screen")
        }
    }

    companion object {
        // TODO 'lazy' as a workaround due to an incompatibility between
        // Fastlane and AndroidX. Remove lazy delegate when this
        // incompatibility has been fixed
        // FFTV issue: #1741
        // Fastlane issue: https://github.com/fastlane/fastlane/issues/13810
        @get:ClassRule
        val localeTestRule by lazy { LocaleTestRule() }
    }
}
