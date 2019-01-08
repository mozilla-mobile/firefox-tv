/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import mozilla.components.browser.session.Session
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.SafeIntent
import org.mozilla.tv.firefox.utils.UrlUtils

data class ValidatedIntentData(val url: String, val source: Session.Source)

/**
 * A container for functions that parse Intents and notify the application of their validity.
 *
 * This class uses callbacks to notify the application in order to decouple the Intent parsing
 * from the application logic, which also allows us to test the functionality.
 *
 * The functions in this class take [SafeIntent] in order to encourage the caller to use SafeIntents
 * in their code.
 */
object IntentValidator {
    @VisibleForTesting const val DIAL_PARAMS_KEY = "com.amazon.extra.DIAL_PARAM"

    /**
     * Validate that [Intent] contains all expected parameters.
     * Returns null if any unexpected values are encountered
     */
    fun validateOnCreate(context: Context, intent: SafeIntent, savedInstanceState: Bundle?): ValidatedIntentData? {
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // This Intent was launched from history (recent apps). Android will redeliver the
            // original Intent (which might be a VIEW intent). However if there's no active browsing
            // session then we do not want to re-process the Intent and potentially re-open a website
            // from a session that the user already "erased".
            return null
        }

        if (savedInstanceState != null) {
            // onNewIntent will handle restoring any state.
            return null
        }

        return validate(context, intent)
    }

    fun validate(context: Context, intent: SafeIntent): ValidatedIntentData? {
        val action = intent.action

        when (action) {
            Intent.ACTION_MAIN -> {
                val dialParams = intent.extras?.getString(DIAL_PARAMS_KEY) ?: return null
                if (dialParams.isNotEmpty()) {
                    TelemetryIntegration.INSTANCE.youtubeCastEvent()
                    return ValidatedIntentData(url = "https://www.youtube.com/tv?$dialParams", source = Session.Source.ACTION_VIEW)
                }
            }
            Intent.ACTION_VIEW -> {
                val dataString = intent.dataString
                if (TextUtils.isEmpty(dataString)) {
                    return null // We can't create a session from an Intent without a URL.
                }

                return ValidatedIntentData(dataString, Session.Source.ACTION_VIEW)
            }
            Intent.ACTION_SEND -> {
                val dataString = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (TextUtils.isEmpty(dataString)) {
                    return null
                }

                val isSearch = !UrlUtils.isUrl(dataString)
                val url = if (isSearch) UrlUtils.createSearchUrl(context, dataString) else dataString
                return ValidatedIntentData(url, Session.Source.ACTION_SEND)
            }
        }
        return null
    }
}
