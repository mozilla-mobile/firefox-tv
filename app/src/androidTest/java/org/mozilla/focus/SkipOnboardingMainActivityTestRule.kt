/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import org.mozilla.focus.OnboardingActivity.Companion.ONBOARD_SHOWN_PREF
import org.mozilla.focus.home.pocket.PocketOnboardingActivity.Companion.POCKET_ONBOARDING_SHOWN_PREF

class SkipOnboardingMainActivityTestRule : ActivityTestRule<MainActivity>(MainActivity::class.java) {
    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        val appContext = InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext

        PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(ONBOARD_SHOWN_PREF, true)
                .putBoolean(POCKET_ONBOARDING_SHOWN_PREF, true)
                .apply()
    }
}
