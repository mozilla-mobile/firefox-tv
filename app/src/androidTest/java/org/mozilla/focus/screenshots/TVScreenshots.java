/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.MainActivity;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.mozilla.focus.OnboardingActivity.ONBOARD_SHOWN_PREF;

@RunWith(AndroidJUnit4.class)
public class TVScreenshots extends ScreenshotTest {

    private Intent intent;
    private SharedPreferences.Editor preferencesEditor;
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class,
            false, false);

    @Before
    public void setUp() throws Exception {
        intent = new Intent();

        Context appContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();

        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
    }

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void firstLaunchScreen() throws InterruptedException, UiObjectNotFoundException {
        /* Overwrite the app preference before main activity launch */
        preferencesEditor
                .putBoolean(ONBOARD_SHOWN_PREF, false)
                .apply();

        mActivityTestRule.launchActivity(intent);

        onView(withId(R.id.enable_turbo_mode))
                .check(matches(isDisplayed()));
        onView(withId(R.id.turbo_mode_title))
                .check(matches(isDisplayed()));
        onView(withId(R.id.disable_turbo_mode))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("first-launch");
    }

    @Test
    public void defaultHomeScreen() throws InterruptedException, UiObjectNotFoundException {
        /* capture a screenshot of the default home-screen */
        mActivityTestRule.launchActivity(intent);

        onView(withId(R.id.urlInputView))
                .check(matches(isDisplayed()));
        onView(withId(R.id.homeUrlBar))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("home-screen");
    }

    @Test
    public void browserNavOverlay () throws InterruptedException, UiObjectNotFoundException {
        /* default home-screen in the main activity should be displayed */
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()))
                .perform(typeTextIntoFocusedView("example.com"))
                .perform(pressImeActionButton());

        onView(withId(R.id.webview))
                .check(matches(isDisplayed()));

        mDevice.pressMenu();

        onView(withId(R.id.navCloseHint))
                .check(matches(isDisplayed()));

        onView(withId(R.id.navUrlInput))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("browser-overlay");

        onView(withId(R.id.navButtonHome)).perform(click());
    }

    @Test
    public void settingsView() throws InterruptedException, UiObjectNotFoundException {
        /* default home-screen in the main activity should be displayed */
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()));

        /* visit settings */
        onView(allOf(withId(R.id.settingsButton), isDisplayed()))
                .perform(click());

        /* current settings list view */
        onView(allOf(withId(R.id.container), isDisplayed()));

        ViewInteraction clearButton = onView(
                allOf(withId(R.id.deleteButton), isDisplayed()));

        /* capture a screenshot of the default settings list */
        Screengrab.screenshot("settings");

        /* capture a screenshot of the clear data dialog */
        clearButton.perform(click());

        ViewInteraction confirmClear = onView(
                allOf(withText(R.string.settings_cookies_dialog_content), isDisplayed()))
                .inRoot(isDialog());

        Screengrab.screenshot("clear-all-data");

        confirmClear.perform(pressBack());

        /* capture a screenshot of the privacy notice */
        onView(allOf(withId(R.id.privacyNoticeButton), isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.webview), isDisplayed()));

        Screengrab.screenshot("privacy-notice");

        mDevice.pressBack();
    }

    @Test
    public void unpinTileInHome() throws InterruptedException, UiObjectNotFoundException {
        /* default home-screen in the main activity should be displayed */
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()));

        mDevice.pressDPadDown();
        mDevice.pressMenu();

        onView(withText(R.string.homescreen_tile_remove))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("menu-remove-tile");

        mDevice.pressBack();
    }

    @Test
    public void pinTileFromOverlay() throws InterruptedException, UiObjectNotFoundException {
        /* default home-screen in the main activity should be displayed */
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()))
                .perform(typeTextIntoFocusedView("example.com"))
                .perform(pressImeActionButton());

        onView(withId(R.id.webview))
                .check(matches(isDisplayed()));

        mDevice.pressMenu();

        onView(withId(R.id.pinButton))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.notification_pinned_site))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("overlay-pinned-tile");

        mDevice.pressBack();
        mDevice.pressBack();
    }

    @Test
    public void unpinTileFromOverlay() throws InterruptedException, UiObjectNotFoundException {
    /* default home-screen in the main activity should be displayed */
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.urlInputView), isDisplayed(), hasFocus()))
                .perform(typeTextIntoFocusedView("example.com"))
                .perform(pressImeActionButton());

        onView(withId(R.id.webview))
                .check(matches(isDisplayed()));

        mDevice.pressMenu();

        onView(withId(R.id.pinButton))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText(R.string.notification_unpinned_site))
                .inRoot(withDecorView(not(is(mActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("overlay-unpinned-tile");

        mDevice.pressBack();
        mDevice.pressBack();
    }
}
