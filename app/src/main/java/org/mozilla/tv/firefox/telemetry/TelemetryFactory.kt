/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.components.search.SearchEngineManager
import org.mozilla.tv.firefox.utils.Settings
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

private const val TELEMETRY_APP_NAME_FOCUS_TV = "FirefoxForFireTV"

/**
 * Constructs objects related to telemetry
 */
object TelemetryFactory {

    fun createTelemetry(context: Context): Telemetry {
        val telemetryEnabled = DataUploadPreference.isEnabled(context)

        val configuration = TelemetryConfiguration(context)
                .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                .setAppName(TELEMETRY_APP_NAME_FOCUS_TV)
                .setUpdateChannel(BuildConfig.BUILD_TYPE)
                .setPreferencesImportantForTelemetry(
                        Settings.TRACKING_PROTECTION_ENABLED_PREF,
                        TelemetrySettingsProvider.PREF_CUSTOM_HOME_TILE_COUNT,
                        TelemetrySettingsProvider.PREF_TOTAL_HOME_TILE_COUNT
                )
                .setSettingsProvider(TelemetrySettingsProvider(context))
                .setCollectionEnabled(telemetryEnabled)
                .setUploadEnabled(telemetryEnabled)

        val serializer = JSONPingSerializer()
        val storage = FileTelemetryStorage(configuration, serializer)
        val client = HttpURLConnectionTelemetryClient()
        val scheduler = JobSchedulerTelemetryScheduler()

        return Telemetry(configuration, storage, client, scheduler)
                .addPingBuilder(TelemetryCorePingBuilder(configuration))
                .addPingBuilder(TelemetryMobileEventPingBuilder(configuration))
                .setDefaultSearchProvider(createDefaultSearchProvider(context))
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            SearchEngineManager.getInstance()
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }
}
