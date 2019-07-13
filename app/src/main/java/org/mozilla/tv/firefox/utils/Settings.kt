/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.engine.EngineSession
import org.mozilla.tv.firefox.onboarding.OnboardingActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ChannelConfig
import org.mozilla.tv.firefox.channels.ChannelOnboardingActivity
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.ext.languageAndMaybeCountryMatch

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

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources: Resources = context.resources

    val defaultSearchEngineName: String?
        get() = preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null)

    fun shouldShowTurboModeOnboarding(): Boolean =
            !preferences.getBoolean(OnboardingActivity.ONBOARD_SHOWN_PREF, false)

    fun shouldShowTVOnboarding(localeManager: LocaleManager, context: Context): Boolean {
        val channelConfig = ChannelConfig.getTvGuideConfig(context)
        val currentLocale = localeManager.getCurrentLocale(context)

        return !preferences.getBoolean(ChannelOnboardingActivity.TV_ONBOARDING_SHOWN_PREF, false) &&
                channelConfig.isEnabledInCurrentExperiment &&
                currentLocale.languageAndMaybeCountryMatch(channelConfig.enabledInLocales)
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
