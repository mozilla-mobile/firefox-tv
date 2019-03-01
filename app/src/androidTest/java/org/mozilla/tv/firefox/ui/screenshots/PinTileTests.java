/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.matcher.ViewMatchers;
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
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;


public class PinTileTests extends ScreenshotTest {

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
    public void unpinTileFromContextMenu() {
        onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()));

        mDevice.pressDPadDown();
        mDevice.pressDPadDown();

        onView(withText(R.string.homescreen_unpin_tutorial_toast))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("unpin-toast");

        mDevice.waitForIdle();

        onView(ViewMatchers.withId(R.id.tileContainer))
                .perform(actionOnItemAtPosition(0, longClick()));

        onView(withText(R.string.homescreen_tile_remove))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("menu-remove-tile");

        mDevice.pressBack();
    }
}
