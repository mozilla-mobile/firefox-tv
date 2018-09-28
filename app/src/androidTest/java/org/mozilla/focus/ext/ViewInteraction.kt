/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.CoreMatchers.not

fun ViewInteraction.click() = this.perform(ViewActions.click())!!

fun ViewInteraction.assertEnabled() = this.check(matches(isEnabled()))!!

fun ViewInteraction.assertDisabled() = this.check(matches(not(isEnabled())))!!
