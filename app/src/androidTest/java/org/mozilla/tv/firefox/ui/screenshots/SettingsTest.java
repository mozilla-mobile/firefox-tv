/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.tv.firefox.MainActivity;
import org.mozilla.tv.firefox.R;
import org.mozilla.tv.firefox.helpers.MainActivityTestRule;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


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
    public void showSettingsViews() {
        onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()));

        // visit settings
        onView(allOf(withId(R.id.navButtonSettings), isDisplayed()))
                .perform(click());

        // current settings list view
        onView(allOf(withId(R.id.container_bottom), isDisplayed()));

        ViewInteraction clearButton = onView(
                allOf(withId(R.id.deleteButton), isDisplayed()));

        // capture a screenshot of the default settings list
        Screengrab.screenshot("settings");

        // capture a screenshot of the clear data dialog
        clearButton.perform(click());

        onView(allOf(withText(R.string.settings_cookies_dialog_content2), isDisplayed())).inRoot(isDialog());

        Screengrab.screenshot("clear-all-data");

        mDevice.pressBack();

        onView(allOf(withId(R.id.aboutButton), isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.webview), isDisplayed()));
        onView(allOf(withId(R.string.your_rights), isDisplayed()));

        mDevice.waitForIdle();

        Screengrab.screenshot("about-screen");

        mDevice.pressBack();
    }
}
