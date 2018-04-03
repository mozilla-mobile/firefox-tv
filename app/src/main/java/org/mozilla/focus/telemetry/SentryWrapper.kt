/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.User
import org.mozilla.focus.BuildConfig

/**
 * An interface to the Sentry crash reporting SDK. All code that touches the Sentry APIs
 * directly should go in here (like TelemetryWrapper).
 *
 * With the current implementation, to enable Sentry on Beta/Release builds, add a
 * <project-dir>/.sentry_dsn_release file with your key. To enable Sentry on Debug
 * builds, add a .sentry_dsn_debug key and replace the [DataUploadPreference.isEnabled]
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
object SentryWrapper {

    fun init(context: Context) {
        onIsEnabledChanged(context, DataUploadPreference.isEnabled(context))
    }

    internal fun onIsEnabledChanged(context: Context, isEnabled: Boolean) {
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
        Sentry.init(sentryDsn, AndroidSentryClientFactory(context.applicationContext))

        // By default, Sentry creates UUIDs for users and we don't want to upload them:
        // they're personally identifiable information.
        Sentry.getContext().user = SentryUnknownUser()
    }
}

/**
 * A Sentry user that does not contain identifiable information like UUID.
 *
 * Use this instead of [io.sentry.context.Context.clearUser] because that
 * will just clear any user we set and use a Sentry-generated UUID instead.
 */
private class SentryUnknownUser : User(null, null, null, null)
