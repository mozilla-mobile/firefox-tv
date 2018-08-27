/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import mozilla.components.browser.session.Session
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.UrlUtils

typealias OnValidBrowserIntent = (url: String, source: Session.Source) -> Unit

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

    fun validateOnCreate(context: Context, intent: SafeIntent, savedInstanceState: Bundle?, onValidBrowserIntent: OnValidBrowserIntent) {
        if ((intent.getFlags() and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // This Intent was launched from history (recent apps). Android will redeliver the
            // original Intent (which might be a VIEW intent). However if there's no active browsing
            // session then we do not want to re-process the Intent and potentially re-open a website
            // from a session that the user already "erased".
            return
        }

        if (savedInstanceState != null) {
            // We are restoring a previous session - No need to handle this Intent.
            return
        }

        validate(context, intent, onValidBrowserIntent)
    }

    fun validate(context: Context, intent: SafeIntent, onValidBrowserIntent: OnValidBrowserIntent) {
        val action = intent.action

        if (Intent.ACTION_VIEW.equals(action)) {
            val dataString = intent.getDataString()
            if (TextUtils.isEmpty(dataString)) {
                return // If there's no URL in the Intent then we can't create a session.
            }

            onValidBrowserIntent(dataString, Session.Source.ACTION_VIEW)
        } else if (Intent.ACTION_SEND.equals(action)) {
            val dataString = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (TextUtils.isEmpty(dataString)) {
                return
            }

            val isSearch = !UrlUtils.isUrl(dataString)
            val url = if (isSearch) UrlUtils.createSearchUrl(context, dataString) else dataString
            onValidBrowserIntent(url, Session.Source.ACTION_SEND)
        }
    }
}
