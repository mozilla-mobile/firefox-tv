/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots;

import android.os.Build;

import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.tv.firefox.MainActivity;
import org.mozilla.tv.firefox.helpers.NestedScrollToAction;
import org.mozilla.tv.firefox.R;
import org.mozilla.tv.firefox.helpers.MainActivityTestRule;

import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

public class TooltipCaptureTest extends ScreenshotTest {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityTestRule();

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void showToolTips() throws InterruptedException {

        ViewInteraction pinnedTileChannel = onView(withId(R.id.pinned_tiles_channel));

        onView(allOf(withId(R.id.navUrlInput), hasFocus())).check(matches(isDisplayed()));
        pinnedTileChannel.perform(new NestedScrollToAction());
        pinnedTileChannel.check(matches(isDisplayed()));

        // open two sites to enable back and front button
        pinnedTileChannel.perform(new NestedScrollToAction(), actionOnItemAtPosition(1, click()));
        device.wait(Until.findObject(By.res(Integer.toString(R.id.progressAnimation))), 2000);
        device.pressMenu();

        pinnedTileChannel.perform(new NestedScrollToAction(), actionOnItemAtPosition(2, click()));
        device.wait(Until.findObject(By.res(Integer.toString(R.id.progressAnimation))), 2000);

        device.pressMenu();
        device.pressDPadUp();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-backbutton", 500);

        onView(withId(R.id.navButtonBack)).perform(click());
        device.wait(Until.findObject(By.res(Integer.toString(R.id.progressAnimation))), 2000);
        device.pressMenu();
        device.pressDPadUp();
        device.pressDPadRight();  // In fastlane, back button is still enabled - this might be fastlane issue

        // forward button
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-forwardbutton", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-reload", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-pintohs", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-turbomode", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-requestdesktop", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-exit", 500);
        device.pressDPadRight();
        checkTooltipDisplayed();
        takeScreenshotsAfterWait("tooltip-settings", 500);
        device.pressDPadLeft();
        device.pressDPadCenter();

        device.wait(Until.findObject(By.text(getString(R.string.notification_request_desktop_site))), 1000);
        takeScreenshotsAfterWait("desktoprequested", 0);
        device.pressMenu();
        device.pressDPadUp();
        device.pressDPadRight();
        device.pressDPadRight();
        device.pressDPadRight();
        device.pressDPadCenter();
        device.wait(Until.findObject(By.text(getString(R.string.notification_request_non_desktop_site))), 1000);
        takeScreenshotsAfterWait("nondesktoprequested", 0);
    }

    private void checkTooltipDisplayed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            onView(withId(R.id.tooltip)).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
        } else {
            onView(withId(R.id.tooltip)).inRoot(withDecorView(withId(R.id.tooltip))).check(matches(isDisplayed()));
        }
    }
}
