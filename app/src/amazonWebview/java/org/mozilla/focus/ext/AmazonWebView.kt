package org.mozilla.focus.ext

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.amazon.android.webkit.AmazonWebView

fun AmazonWebView.deleteData() {
    clearFormData();
    clearHistory();
    clearMatches();
    clearSslPreferences();
    clearCache(true);

    // We don't care about the callback - we just want to make sure cookies are gone
    CookieManager.getInstance().removeAllCookies(null);

    WebStorage.getInstance().deleteAllData();

    val webViewDatabase = WebViewDatabase.getInstance(context);
    webViewDatabase.clearFormData(); // Unclear how this differs from WebView.clearFormData()
    webViewDatabase.clearHttpAuthUsernamePassword();

    deleteContentFromKnownLocations(context);
}

fun deleteContentFromKnownLocations(context: Context) {
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
