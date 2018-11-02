/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.web.webdriver.Locator
import android.support.test.rule.ActivityTestRule
import android.support.test.uiautomator.UiDevice

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.hasFocus
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.view.View
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.StringContains.containsString

class PageLoadTest {

    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = MainActivityTestRule()

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun PageLoadTest() {
        onView(ViewMatchers.withId(R.id.tileContainer))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(TILE_POSITION, click()))

        onView(ViewMatchers.withId(R.id.webview))
            .check(matches(isDisplayed()))

        onWebView()
            .withElement(findElement(Locator.ID, TILE_WEBSITE_ELEMENT))

        mDevice.pressMenu()

        onView(ViewMatchers.withId(R.id.navUrlInput))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("google"))))

        onView(allOf<View>(withId(R.id.navUrlInput), isDisplayed(), hasFocus()))
            .perform(replaceText(MOZILLA_URL))
            .perform(pressImeActionButton())

        onView(ViewMatchers.withId(R.id.webview))
            .check(matches(isDisplayed()))

        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, MOZILLA_PAGE_ELEMENT))

        mDevice.pressMenu()

        onView(ViewMatchers.withId(R.id.navUrlInput))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("mozilla"))))
    }

    companion object {
        private val TILE_POSITION = 1 // Google Video Search
        private val TILE_WEBSITE_ELEMENT = "hplogo" // Google logo
        private val MOZILLA_URL = "mozilla.org"
        private val MOZILLA_PAGE_ELEMENT = ".primary-title"
    }
}
