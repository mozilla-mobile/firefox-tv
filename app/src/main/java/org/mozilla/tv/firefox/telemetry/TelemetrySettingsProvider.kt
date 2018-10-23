/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import org.mozilla.tv.firefox.pinnedtile.BundledTilesManager
import org.mozilla.tv.firefox.pinnedtile.CustomTilesManager
import org.mozilla.telemetry.measurement.SettingsMeasurement

/**
 * A SettingsProvider that provides custom value getters when settings values
 * can't just be retrieved from SharedPreferences.
 */
class TelemetrySettingsProvider(private val appContext: Context) : SettingsMeasurement.SharedPreferenceSettingsProvider() {

    override fun containsKey(key: String?): Boolean {
        return key == PREF_CUSTOM_HOME_TILE_COUNT ||
                key == PREF_TOTAL_HOME_TILE_COUNT ||
                super.containsKey(key)
    }

    override fun getValue(key: String?): Any? = when (key) {
        PREF_CUSTOM_HOME_TILE_COUNT -> CustomTilesManager.getInstance(appContext).tileCount
        PREF_TOTAL_HOME_TILE_COUNT -> CustomTilesManager.getInstance(appContext).tileCount +
                BundledTilesManager.getInstance(appContext).tileCount
        else -> super.getValue(key)
    }

    companion object {
        // These keys are technically not settings, but they're more naturally recorded
        // in the event ping's aggregate region than they are as UI events.
        const val PREF_CUSTOM_HOME_TILE_COUNT = "custom_home_tile_count"
        const val PREF_TOTAL_HOME_TILE_COUNT = "total_home_tile_count"
    }
}
