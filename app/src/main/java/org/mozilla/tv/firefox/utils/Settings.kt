/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import android.support.annotation.VisibleForTesting
import mozilla.components.concept.engine.EngineSession
import org.mozilla.tv.firefox.OnboardingActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.home.pocket.PocketOnboardingActivity
import org.mozilla.tv.firefox.search.SearchEngine

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
class Settings private constructor(context: Context) {
    companion object {
        private var instance: Settings? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): Settings {
            if (instance == null) {
                instance = Settings(context.applicationContext)
            }
            return instance ?: throw AssertionError("Instance cleared")
        }

        const val TRACKING_PROTECTION_ENABLED_PREF = "tracking_protection_enabled"
        const val TRACKING_PROTECTION_ENABLED_DEFAULT = true

        @VisibleForTesting internal fun reset() {
            instance = null
        }
    }

    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources: Resources = context.resources

    val defaultSearchEngineName: String?
        get() = preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null)

    fun shouldShowTurboModeOnboarding(): Boolean =
            !preferences.getBoolean(OnboardingActivity.ONBOARD_SHOWN_PREF, false)

    fun shouldShowPocketOnboarding(): Boolean =
            !preferences.getBoolean(PocketOnboardingActivity.POCKET_ONBOARDING_SHOWN_PREF, false)

    fun setDefaultSearchEngine(searchEngine: SearchEngine) {
        preferences.edit()
                .putString(getPreferenceKey(R.string.pref_key_search_engine), searchEngine.name)
                .apply()
    }

    fun shouldAutocompleteFromShippedDomainList() = true

    private fun getPreferenceKey(resourceId: Int): String =
            resources.getString(resourceId)

    // Accessible via TurboMode.isEnabled()
    internal var isBlockingEnabled: Boolean // Delegates to shared prefs; could be custom delegate.
        get() = preferences.getBoolean(Settings.TRACKING_PROTECTION_ENABLED_PREF,
                TRACKING_PROTECTION_ENABLED_DEFAULT)
        set(value) = preferences.edit().putBoolean(TRACKING_PROTECTION_ENABLED_PREF, value).apply()

    /**
     * Get the tracking protection policy which is a combination of tracker categories that should be blocked.
     */
    val trackingProtectionPolicy: EngineSession.TrackingProtectionPolicy
        get() {
            return if (isBlockingEnabled) {
                EngineSession.TrackingProtectionPolicy.select(
                    EngineSession.TrackingProtectionPolicy.AD,
                    EngineSession.TrackingProtectionPolicy.ANALYTICS,
                    EngineSession.TrackingProtectionPolicy.SOCIAL
                )
            } else {
                EngineSession.TrackingProtectionPolicy.none()
            }
        }
}
