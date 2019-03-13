/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.preference.PreferenceManager
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.onboarding.OnboardingActivity.Companion.ONBOARD_SHOWN_PREF
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity.Companion.POCKET_ONBOARDING_SHOWN_PREF

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [MainActivity].
 *
 * @param initialTouchMode See [ActivityTestRule]
 * @param launchActivity See [ActivityTestRule]
 * @param skipOnboarding true to skip the onboarding screen, false otherwise
 */
class MainActivityTestRule(
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true,
    private val skipOnboarding: Boolean = true
) : ActivityTestRule<MainActivity>(MainActivity::class.java, initialTouchMode, launchActivity) {

    /**
     * Ensures the test doesn't advance until session page load is completed.
     *
     * N.B.: in the current implementation, tests pass without this so it seems to be
     * unnecessary: I think this is because the progress bar animation acts as the
     * necessary idling resource. However, we leave this in just in case the
     * implementation changes and the tests break. In that case, this code might be
     * broken because it's not used, and thus tested, at present.
     */
    lateinit var loadingIdlingResource: SessionLoadedIdlingResource
        private set

    override fun beforeActivityLaunched() {
        if (skipOnboarding) {
            skipOnboarding()
        }

        loadingIdlingResource = SessionLoadedIdlingResource().also {
            IdlingRegistry.getInstance().register(it)
        }
    }

    override fun afterActivityFinished() {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource)
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
