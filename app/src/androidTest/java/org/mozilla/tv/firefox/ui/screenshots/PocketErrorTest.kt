/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.screenshots

import android.app.Application
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.view.View
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.TestDependencyFactory
import org.mozilla.tv.firefox.helpers.CustomPocketFeedStateProvider
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.utils.ServiceLocator
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class PocketErrorTest : ScreenshotTest() {

    companion object : TestDependencyFactory {
        private val customPocketFeedStateProvider = CustomPocketFeedStateProvider()

        // TODO 'lazy' as a workaround due to an incompatibility between
        // Fastlane and AndroidX. Remove lazy delegate when this
        // incompatibility has been fixed
        // FFTV issue: #1741
        // Fastlane issue: https://github.com/fastlane/fastlane/issues/13810
        @get:ClassRule
        val localeTestRule by lazy { LocaleTestRule() }

        override fun createServiceLocator(app: Application) = object : ServiceLocator(app) {
            override val pocketRepo = customPocketFeedStateProvider.fakedPocketRepo
        }
    }

    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = MainActivityTestRule()

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun showPocketTileError() {
        customPocketFeedStateProvider.fakedPocketRepoState.onNext(PocketVideoRepo.FeedState.FetchFailed)

        val errorMsg = mDevice.findObject(
            UiSelector()
                .resourceId("org.mozilla.tv.firefox.debug:id/pocketMegaTileLoadError")
                .enabled(true)
        )

        errorMsg.waitForExists(5000)

        Screengrab.screenshot("pocket-tile-error")

        onView(allOf<View>(withId(R.id.megaTileTryAgainButton), isDisplayed()))
            .perform(click())
    }
}
