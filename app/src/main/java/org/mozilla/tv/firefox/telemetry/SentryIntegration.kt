/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.NONE
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.settings.SettingsRepo

/**
 * An interface to the Sentry crash reporting SDK. All code that touches the Sentry APIs
 * directly should go in here (like TelemetryWrapper).
 *
 * With the current implementation, to enable Sentry on Release builds, add a
 * <project-dir>/.sentry_dsn_release file with your key. To enable Sentry on Debug
 * builds, add a .sentry_dsn_debug key and in this file replace check for [isEnabled]
 * value with true (upload is disabled by default in dev builds). These keys are available
 * in the APT Google Drive -> Fire TV -> Engineering -> Secrets dir.
 *
 * The gradle output is the only way to verify if adding the key was successful (but it
 * won't indicate if the key is valid: #747). You will see a message in the gradle output
 * indicating the key was added:
 * "Sentry DSN (amazonWebviewRelease): Added from /Users/mcomella/dev/moz/firefox-tv/.sentry_dsn_release"
 * As opposed to:
 * "Sentry DSN (amazonWebviewRelease): X_X"
 */
object SentryIntegration {

    @VisibleForTesting(otherwise = NONE)
    var isInit = false
        private set

    /**
     * Initializes Sentry. This method should only be called once.
     */
    fun init(appContext: Context, settingsRepo: SettingsRepo) {
        isInit = true

        // This listener binds to the Context and observes forever so it's important
        // that we use an appContext to avoid memory leaks.
        settingsRepo.dataCollectionEnabled.observeForever { isEnabled ->
            if (isEnabled != null) {
                // The BuildConfig value is populated from a file at compile time.
                // If the file did not exist, the value will be null.
                //
                // If you provide a null DSN to Sentry, it will disable upload and buffering to disk:
                // https://github.com/getsentry/sentry-java/issues/574#issuecomment-378298484
                //
                // In the current implementation, each time `init` is called, it will overwrite the
                // stored client and DSN, thus calling it with a null DSN will have the affect of
                // disabling the client: https://github.com/getsentry/sentry-java/issues/574#issuecomment-378406105
                val sentryDsn = if (isEnabled) BuildConfig.SENTRY_DSN else null
                Sentry.init(sentryDsn, AndroidSentryClientFactory(appContext))
            }
        }
    }

    /**
     * Sends the given [exception] to the Sentry servers without crashing the app.
     *
     * @see [Sentry.capture]
     */
    fun capture(exception: Exception) {
        Sentry.capture(exception)
    }

    /**
     * Sends the given [exception] to the Sentry servers without crashing the app
     * and logs its message at the error level.
     *
     * @see [Sentry.capture]
     */
    fun captureAndLogError(tag: String, exception: Exception) {
        // Note: instead, by adding the Log4j dependency, we may be able to get Sentry to log captures to logcat automatically.
        Log.e(tag, exception.message)
        capture(exception)
    }
}
