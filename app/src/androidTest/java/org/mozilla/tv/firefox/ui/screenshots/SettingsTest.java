/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.tv.firefox.MainActivity;
import org.mozilla.tv.firefox.R;
import org.mozilla.tv.firefox.helpers.MainActivityTestRule;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static mozilla.components.support.android.test.espresso.matcher.ViewMatchersKt.hasFocus;
import static org.hamcrest.core.AllOf.allOf;


public class SettingsTest extends ScreenshotTest {

    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityTestRule();

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void showSettingsUsageData() throws InterruptedException {
        linearNavigateToSettingsChannel();

        // capture a screenshot of the default settings list
        Screengrab.screenshot("settings");

        mDevice.pressDPadCenter();
        takeScreenshotsAfterWait("send-usage-data", 5000);
    }

    @Test
    public void showSettingsClearAllData() throws InterruptedException {
        linearNavigateToSettingsChannel();
        mDevice.pressDPadRight();

        onView(withId(R.id.settings_tile_cleardata)).check(matches(isDisplayed()));
        mDevice.pressDPadCenter();
        takeScreenshotsAfterWait("clear-all-data", 5000);
    }

    @Test
    public void showSettingsAboutScreen() throws InterruptedException {
        linearNavigateToSettingsChannel();
        mDevice.pressDPadRight();
        mDevice.pressDPadRight();

        onView(withId(R.id.settings_tile_about)).check(matches(isDisplayed()));
        mDevice.pressDPadCenter();
        takeScreenshotsAfterWait("about-screen", 5000);
    }

    void linearNavigateToSettingsChannel() {

        boolean settingsTileSelected = false;

        while (!settingsTileSelected) {
            try {
                onView(allOf(withId(R.id.settings_cardview), hasFocus(true))).check(matches(isDisplayed()));
                settingsTileSelected = true;
            } catch (NoMatchingViewException ex) {
                mDevice.pressDPadDown();
            }
        }
    }
}
