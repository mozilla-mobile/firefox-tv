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
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.MainActivity;
import org.mozilla.focus.R;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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
import static org.mozilla.focus.home.pocket.PocketOnboardingActivity.POCKET_ONBOARDING_SHOWN_PREF;


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
    public void setUp() {
        intent = new Intent();

        Context appContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();

        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();

        PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .clear()
                .putBoolean(ONBOARD_SHOWN_PREF, true)
                .putBoolean(POCKET_ONBOARDING_SHOWN_PREF, true)
                .apply();
    }

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void firstLaunchScreen() {
        // Overwrite the app preference before main activity launch
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
    public void pocketOnboarding() {
        // Overwrite the app preference before main activity launch
        preferencesEditor
                .putBoolean(POCKET_ONBOARDING_SHOWN_PREF, false)
                .apply();

        mActivityTestRule.launchActivity(intent);

        onView(withId(R.id.pocket_onboarding_button))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("pocket-onboarding");

        mDevice.pressDPadCenter();
    }

    @Test
    public void defaultHomeScreen() {
        // capture a screenshot of the default home-screen
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()));

        onView(allOf(withId(R.id.tileContainer), isDisplayed()));

        Screengrab.screenshot("home-screen");
    }

    @Test
    public void pocketRecommendation() {
        // default home-screen in the main activity should be displayed
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.pocketVideoMegaTileView), isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.videoFeed), isDisplayed()));
        onView(allOf(withId(R.id.recommendedTitle), isDisplayed()));

        Screengrab.screenshot("pocket-default-recommendations");

        mDevice.pressBack();
    }

    @Test
    public void settingsView() {
        // default home-screen in the main activity should be displayed
        mActivityTestRule.launchActivity(intent);

        onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()));

        // visit settings
        onView(allOf(withId(R.id.navButtonSettings), isDisplayed()))
                .perform(click());

        // current settings list view
        onView(allOf(withId(R.id.container), isDisplayed()));

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

    @Test
    public void unpinTileFromContextMenu() {
        // default home-screen in the main activity should be displayed
        mActivityTestRule.launchActivity(intent);

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
