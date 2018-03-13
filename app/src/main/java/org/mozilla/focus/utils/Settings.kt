/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager

import org.mozilla.focus.R
import org.mozilla.focus.activity.OnboardingActivity
import org.mozilla.focus.search.SearchEngine
import org.mozilla.focus.web.IWebView

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
    }

    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources: Resources = context.resources

    val defaultSearchEngineName: String?
        get() = preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null)

    fun shouldShowOnboarding(): Boolean =
            !preferences.getBoolean(OnboardingActivity.ONBOARD_SHOWN_PREF, false)

    fun setDefaultSearchEngine(searchEngine: SearchEngine) {
        preferences.edit()
                .putString(getPreferenceKey(R.string.pref_key_search_engine), searchEngine.name)
                .apply()
    }

    fun shouldAutocompleteFromShippedDomainList() = true

    private fun getPreferenceKey(resourceId: Int): String =
            resources.getString(resourceId)

    var isBlockingEnabled: Boolean // Delegates to shared prefs; could be custom delegate.
        get() = preferences.getBoolean(IWebView.TRACKING_PROTECTION_ENABLED_PREF,
                IWebView.TRACKING_PROTECTION_ENABLED_DEFAULT)
        set(value) = preferences.edit().putBoolean(IWebView.TRACKING_PROTECTION_ENABLED_PREF, value).apply()
}