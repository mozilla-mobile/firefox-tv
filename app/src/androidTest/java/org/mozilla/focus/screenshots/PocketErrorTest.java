/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshots;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.MainActivity;
import org.mozilla.focus.R;

import tools.fastlane.screengrab.Screengrab;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.mozilla.focus.OnboardingActivity.ONBOARD_SHOWN_PREF;
import static org.mozilla.focus.home.pocket.PocketOnboardingActivity.POCKET_ONBOARDING_SHOWN_PREF;


@RunWith(AndroidJUnit4.class)
public class PocketErrorTest extends ScreenshotTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(ONBOARD_SHOWN_PREF, true)
                    .putBoolean(POCKET_ONBOARDING_SHOWN_PREF, true)
                    .apply();
        }
    };

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void showPocketTileError() {
        onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()))
                .perform(replaceText("firefox:error:pocketconnection"))
                .perform(pressImeActionButton());

        onView(ViewMatchers.withId(R.id.pocketMegaTileLoadError))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("pocket-tile-error");
    }
}
