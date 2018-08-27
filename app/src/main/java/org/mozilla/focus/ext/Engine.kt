/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import mozilla.components.concept.engine.Engine

// Extension methods on the Engine class. This is used for additional features that are not part
// of the upstream browser-engine(-system) component yet.

/**
 * Delete all engine data.
 *
 * Upstream components issue:
 * https://github.com/mozilla-mobile/android-components/issues/644
 */
@Suppress("DEPRECATION") // To be safe, we'll use delete methods as long as they're there.
fun Engine.deleteData(context: Context) {
    WebView(context).apply {
        clearFormData()
        clearHistory()
        clearMatches()
        clearSslPreferences()
        clearCache(true)

        // We don't care about the callback - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null)

        WebStorage.getInstance().deleteAllData()

        val webViewDatabase = WebViewDatabase.getInstance(context)
        webViewDatabase.clearFormData() // Unclear how this differs from WebView.clearFormData()
        webViewDatabase.clearHttpAuthUsernamePassword()
    }
}
