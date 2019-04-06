/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.ViewInteraction
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice

import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule

import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf


class SettingsTest : ScreenshotTest() {

    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = MainActivityTestRule()

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun showSettingsViews() {
        onView(allOf<View>(withId(R.id.navUrlInput), isDisplayed(), hasFocus()))

        // TODO: scroll down to settings channel
        // current settings list view
        onView(allOf<View>(withId(R.id.container_web_render), isDisplayed()))

        // capture a screenshot of the default settings list
        Screengrab.screenshot("settings")

        // TODO: Open Telemetry section
        // TODO: Open Clear Data section
        // capture a screenshot of the clear data dialog
        //        clearButton.perform(click());

        onView(allOf<View>(withText(R.string.settings_cookies_dialog_content2), isDisplayed())).inRoot(isDialog())

        Screengrab.screenshot("clear-all-data")

        mDevice.pressBack()

        // TODO: Open About section
        //        onView(allOf(withId(R.id.aboutButton), isDisplayed()))
        //                .perform(click());

        onView(allOf<View>(withId(R.id.engineView), isDisplayed()))
        onView(allOf<View>(withId(R.string.your_rights), isDisplayed()))

        mDevice.waitForIdle()

        Screengrab.screenshot("about-screen")

        mDevice.pressBack()
    }

    companion object {

        @ClassRule
        val localeTestRule = LocaleTestRule()
    }
}
