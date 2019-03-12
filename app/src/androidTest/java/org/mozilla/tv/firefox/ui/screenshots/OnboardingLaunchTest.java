/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
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
import static org.mozilla.tv.firefox.onboarding.OnboardingActivity.ONBOARD_SHOWN_PREF;


public class OnboardingLaunchTest extends ScreenshotTest {

    private Intent intent;
    private SharedPreferences.Editor preferencesEditor;

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityTestRule(false, false, false);

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
}
