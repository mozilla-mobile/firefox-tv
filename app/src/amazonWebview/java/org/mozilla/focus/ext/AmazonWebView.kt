/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.amazon.android.webkit.AmazonWebView

@Suppress("DEPRECATION") // To be safe, we'll use delete methods as long as they're there.
fun AmazonWebView.deleteData() {
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

    deleteContentFromKnownLocations(context)
}

private fun deleteContentFromKnownLocations(context: Context) {
    /*
    ThreadUtils.postToBackgroundThread(new Runnable() {
        @Override
        public void run() {
            // We call all methods on WebView to delete data. But some traces still remain
            // on disk. This will wipe the whole webview directory.
            FileUtils.deleteWebViewDirectory(context);

            // WebView stores some files in the cache directory. We do not use it ourselves
            // so let's truncate it.
            FileUtils.truncateCacheDirectory(context);
        }
    });
    */
}
