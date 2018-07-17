/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.model.Atoms.getCurrentUrl;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mozilla.focus.OnboardingActivity.ONBOARD_SHOWN_PREF;

@RunWith(AndroidJUnit4.class)
public class BrowserOverlayTest {

    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

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
                    .apply();
        }
    };

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void testBrowserOverlay() throws InterruptedException, UiObjectNotFoundException {
        final ViewInteraction URLBar = onView(allOf(withId(R.id.navUrlInput), isDisplayed(), hasFocus()))
                .perform(typeTextIntoFocusedView("example.com"))
                .perform(pressImeActionButton());

        final ViewInteraction webView;
        webView = onView(ViewMatchers.withId(R.id.webview))
                .check(matches(isDisplayed()));

        mDevice.pressMenu();

        /* Defaults for a website that is non-tile */

        onView(ViewMatchers.withId(R.id.turboButton))
                .check(matches(isChecked()));

        onView(ViewMatchers.withId(R.id.pinButton))
                .check(matches(isNotChecked()));

        /* Home Button */

        onView(ViewMatchers.withId(R.id.navButtonHome))
                .check(matches(isDisplayed()))
                .perform(click());

        URLBar
                .perform(typeTextIntoFocusedView("example.com"))
                .perform(pressImeActionButton());

        webView.check(matches(isDisplayed()));

        mDevice.pressMenu();

        /* Settings */

        onView(ViewMatchers.withId(R.id.navButtonSettings))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(ViewMatchers.withId(R.id.container))
                .check(matches(isDisplayed()));

        pressBack();

        webView.check(matches(isDisplayed()));

        /* Navigation: visit/back/forward */

        onView(ViewMatchers.withId(R.id.navUrlInput))
                .check(matches(isDisplayed()))
                .perform(click())
                .perform(replaceText("example.org"))
                .perform(pressImeActionButton());

        onWebView()
                .check(webMatches(getCurrentUrl(), containsString("example.org")));

        mDevice.pressMenu();

        onView(ViewMatchers.withId(R.id.navUrlInput))
                .check(matches(withText(containsString("example.org"))));

        onView(ViewMatchers.withId(R.id.pinButton))
                .check(matches(isNotChecked()));

        onView(ViewMatchers.withId(R.id.navButtonBack))
                .check(matches(isDisplayed()))
                .perform(click());

        mDevice.pressMenu();

        onWebView()
                .check(webMatches(getCurrentUrl(), containsString("example.com")));

        mDevice.pressMenu();

        onView(ViewMatchers.withId(R.id.navButtonForward))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(ViewMatchers.withId(R.id.navUrlInput))
                .check(matches(withText(containsString("example.org"))));
    }
}
