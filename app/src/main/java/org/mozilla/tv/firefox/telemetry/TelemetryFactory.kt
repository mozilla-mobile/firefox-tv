/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION") // TODO remove this annotation as soon as we migrate to Glean

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import mozilla.components.concept.fetch.Client
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.net.TelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.utils.HttpUrlConnectionWrapper
import org.mozilla.tv.firefox.utils.Settings

private const val TELEMETRY_APP_NAME_FOCUS_TV = "FirefoxForFireTV"

/**
 * Constructs objects related to telemetry
 */
object TelemetryFactory {
    var client: Client = HttpUrlConnectionWrapper.client // Set it to [TestClient] to intercept request payload

    fun createTelemetry(context: Context): Telemetry {
        val configuration = TelemetryConfiguration(context)
                .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                .setAppName(TELEMETRY_APP_NAME_FOCUS_TV)
                .setUpdateChannel(BuildConfig.BUILD_TYPE)
                .setPreferencesImportantForTelemetry(
                        Settings.TRACKING_PROTECTION_ENABLED_PREF,
                        TelemetrySettingsProvider.PREF_CUSTOM_HOME_TILE_COUNT,
                        TelemetrySettingsProvider.PREF_TOTAL_HOME_TILE_COUNT,
                        TelemetrySettingsProvider.PREF_REMOTE_CONTROL_NAME,
                        TelemetrySettingsProvider.APP_ID
                )
                .setSettingsProvider(TelemetrySettingsProvider(context))

        context.serviceLocator.settingsRepo.dataCollectionEnabled.observeForever { collectionEnabled ->
            if (collectionEnabled != null) {
                configuration.isUploadEnabled = collectionEnabled
                configuration.isCollectionEnabled = collectionEnabled
            }
        }

        val serializer = JSONPingSerializer()
        val storage = FileTelemetryStorage(configuration, serializer)
        val telemetryClient = TelemetryClient(client)
        val scheduler = JobSchedulerTelemetryScheduler()

        return Telemetry(configuration, storage, telemetryClient, scheduler)
                .addPingBuilder(TelemetryCorePingBuilder(configuration))
                .addPingBuilder(TelemetryMobileEventPingBuilder(configuration))
                .setDefaultSearchProvider(createDefaultSearchProvider(context))
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            context.serviceLocator.searchEngineManager
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }
}
