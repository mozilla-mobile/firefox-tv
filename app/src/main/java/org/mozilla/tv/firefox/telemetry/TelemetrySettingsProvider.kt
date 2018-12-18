/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import org.mozilla.telemetry.measurement.SettingsMeasurement
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.serviceLocator

/**
 * A SettingsProvider that provides custom value getters when settings values
 * can't just be retrieved from SharedPreferences.
 */
class TelemetrySettingsProvider(private val appContext: Context) : SettingsMeasurement.SharedPreferenceSettingsProvider() {

    override fun containsKey(key: String?): Boolean {
        return key == PREF_CUSTOM_HOME_TILE_COUNT ||
            key == PREF_TOTAL_HOME_TILE_COUNT ||
            key == APP_ID ||
            super.containsKey(key)
    }

    override fun getValue(key: String?): Any? = when (key) {
        PREF_CUSTOM_HOME_TILE_COUNT -> appContext.serviceLocator.pinnedTileRepo.customTilesSize
        PREF_TOTAL_HOME_TILE_COUNT -> appContext.serviceLocator.pinnedTileRepo.customTilesSize +
                appContext.serviceLocator.pinnedTileRepo.bundledTilesSize
        APP_ID -> BuildConfig.APPLICATION_ID
        else -> super.getValue(key)
    }

    companion object {
        // These keys are technically not settings, but they're more naturally recorded
        // in the event ping's aggregate region than they are as UI events.
        const val PREF_CUSTOM_HOME_TILE_COUNT = "custom_home_tile_count"
        const val PREF_TOTAL_HOME_TILE_COUNT = "total_home_tile_count"
        const val APP_ID = "app_id"
    }
}
