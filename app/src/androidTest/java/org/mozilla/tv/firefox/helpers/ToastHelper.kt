/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.support.annotation.StringRes
import android.support.test.espresso.Espresso
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers

object ToastHelper {
    fun assertToast(@StringRes textId: Int, activityTestRule: ActivityTestRule<*>) = Espresso.onView(
        ViewMatchers.withText(
            textId
        )
    )
        .inRoot(RootMatchers.withDecorView(CoreMatchers.not(CoreMatchers.`is`(activityTestRule.activity.window.decorView))))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
}
