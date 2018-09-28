/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Suppress for literal UA comment below. detekt doesn't support lower-level annotations
// for MaxLineLength: https://github.com/arturbosch/detekt/issues/715
@file:Suppress("MaxLineLength")

package org.mozilla.focus.browser

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.text.TextUtils

/** A collection of user agent functionality. */
object UserAgent {

    /**
     * Build the browser specific portion of the UA String, based on the webview's existing UA String.
     */
    @VisibleForTesting
    internal fun getUABrowserString(existingUAString: String, focusToken: String): String {
        // Use the default WebView agent string here for everything after the platform, but insert
        // Focus in front of Chrome.
        // E.g. a default webview UA string might be:
        // Mozilla/5.0 (Linux; Android 7.1.1; Pixel XL Build/NOF26V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/56.0.2924.87 Mobile Safari/537.36
        // And we reuse everything from AppleWebKit onwards, except for adding Focus.
        var start = existingUAString.indexOf("AppleWebKit")
        if (start == -1) {
            // I don't know if any devices don't include AppleWebKit, but given the diversity of Android
            // devices we should have a fallback: we search for the end of the platform String, and
            // treat the next token as the start:
            start = existingUAString.indexOf(")") + 2

            // If this was located at the very end, then there's nothing we can do, so let's just
            // return focus:
            if (start >= existingUAString.length) {
                return focusToken
            }
        }

        val tokens = existingUAString.substring(start).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (i in tokens.indices) {
            if (tokens[i].startsWith("Chrome")) {
                tokens[i] = focusToken + " " + tokens[i]

                return TextUtils.join(" ", tokens)
            }
        }

        // If we didn't find a chrome token, we just append the focus token at the end:
        return TextUtils.join(" ", tokens) + " " + focusToken
    }

    @JvmStatic
    fun buildUserAgentString(context: Context, systemUserAgent: String, appName: String): String {
        val uaBuilder = StringBuilder()

        uaBuilder.append("Mozilla/5.0")

        // WebView by default includes "; wv" as part of the platform string, but we're a full browser
        // so we shouldn't include that.
        // Most webview based browsers (and chrome), include the device name AND build ID, e.g.
        // "Pixel XL Build/NOF26V", that seems unnecessary (and not great from a privacy perspective),
        // so we skip that too.
        uaBuilder.append(" (Linux; Android ").append(Build.VERSION.RELEASE).append(") ")

        val appVersion: String? // unknown if Android framework returns null but not worth crashing over.
        try {
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw IllegalStateException("Unable find package details for Focus", e)
        }

        val focusToken = appName + "/" + appVersion
        uaBuilder.append(getUABrowserString(systemUserAgent, focusToken))

        return uaBuilder.toString()
    }
}
