/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.onboarding.OnboardingActivity.Companion.ONBOARD_SHOWN_PREF
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity.Companion.POCKET_ONBOARDING_SHOWN_PREF

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [MainActivity].
 *
 * @param skipOnboarding true to skip the onboarding screen, false otherwise
 */
class MainActivityTestRule(
        private val skipOnboarding: Boolean = true
) : ActivityTestRule<MainActivity>(MainActivity::class.java) {

    override fun beforeActivityLaunched() {
        if (skipOnboarding) {
            skipOnboarding()
        }
    }

    private fun skipOnboarding() {
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
