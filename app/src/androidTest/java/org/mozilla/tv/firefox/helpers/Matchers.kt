/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.support.test.espresso.matcher.ViewMatchers.isEnabled as espressoIsEnabled
import android.view.View
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher

/**
 * The [android.support.test.espresso.matcher.ViewMatchers.isEnabled] function that can also handle
 * disabled state through the boolean argument.
 */
fun isEnabled(isEnabled: Boolean): Matcher<View> = when {
    isEnabled -> espressoIsEnabled()
    else -> not(espressoIsEnabled())
}
