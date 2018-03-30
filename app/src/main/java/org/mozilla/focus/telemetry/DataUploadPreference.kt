/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import android.os.StrictMode
import android.preference.PreferenceManager
import org.mozilla.focus.R
import org.mozilla.focus.utils.AppConstants
import org.mozilla.telemetry.TelemetryHolder

private const val PREF_KEY_TELEMETRY = R.string.pref_key_telemetry

/** A data container for for the "Send usage data" preference the user can switch. */
internal object DataUploadPreference {

    fun isEnabled(context: Context): Boolean {
        if (AppConstants.isDevBuild()) return false

        // The first access to shared preferences will require a disk read.
        val threadPolicy = StrictMode.allowThreadDiskReads()
        try {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            return preferences.getBoolean(resources.getString(PREF_KEY_TELEMETRY), true)
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    fun setIsEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit()
                .putBoolean(resources.getString(PREF_KEY_TELEMETRY), enabled)
                .apply()

        TelemetryHolder.get()
                .configuration
                .setUploadEnabled(enabled).isCollectionEnabled = enabled

        SentryWrapper.onIsEnabledChanged(context, enabled)
    }
}
