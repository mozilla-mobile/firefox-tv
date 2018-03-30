/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import org.mozilla.focus.BuildConfig

/**
 * An interface to the Sentry crash reporting SDK. All code that touches the Sentry APIs
 * directly should go in here (like TelemetryWrapper).
 *
 * With the current implementation, to enable Sentry on Beta/Release builds, add a
 * <project-dir>/.sentry_dsn_release file with your key. To enable Sentry on Debug
 * builds, add a .sentry_dsn_debug key and replace the [DataUploadPreference.isEnabled]
 * value with true (upload is disabled by default in dev builds).
 */
object SentryWrapper {

    fun init(context: Context) {
        onIsEnabledChanged(context, DataUploadPreference.isEnabled(context))
    }

    internal fun onIsEnabledChanged(context: Context, isEnabled: Boolean) {
        // If the DSN is null, Sentry will attempt to find a DSN from other sources,
        // it'll fail, and it won't be able to upload.
        //
        // The BuildConfig value is populated from a file at compile time.
        // If the file// did not exist, the value will be null.
        val sentryDsn = if (isEnabled) BuildConfig.SENTRY_DSN else null

        // Unfortunately, Sentry doesn't make it easy to disable upload. However,
        // in the current implementation, each time `init` is called, it will overwrite
        // the stored client. If we give it a client with an invalid DSN, it'll have
        // the effect of disabling the client.
        Sentry.init(sentryDsn, AndroidSentryClientFactory(context.applicationContext))
    }
}
