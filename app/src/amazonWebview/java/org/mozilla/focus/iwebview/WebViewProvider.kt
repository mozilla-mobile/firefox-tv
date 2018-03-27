/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.iwebview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.amazon.android.webkit.AmazonWebKitFactory
import com.amazon.android.webkit.AmazonWebSettings
import org.mozilla.focus.R
import org.mozilla.focus.browser.UserAgent
import org.mozilla.focus.ext.hasChild
import org.mozilla.focus.webview.FirefoxAmazonWebChromeClient
import org.mozilla.focus.webview.FirefoxAmazonWebView
import org.mozilla.focus.webview.FocusWebViewClient
import org.mozilla.focus.webview.TrackingProtectionWebViewClient

private val uiHandler = Handler(Looper.getMainLooper())

/** Creates a WebView-based IWebView implementation. */
object WebViewProvider {
    /**
     * Preload webview data. This allows the webview implementation to load resources and other data
     * it might need, in advance of intialising the view (at which time we are probably wanting to
     * show a website immediately).
     */
    @JvmStatic
    fun preload(context: Context) {
        TrackingProtectionWebViewClient.triggerPreload(context)
    }

    @JvmStatic
    fun create(context: Context, attrs: AttributeSet, factory: AmazonWebKitFactory): View {
        val client = FocusWebViewClient(context.applicationContext)
        val chromeClient = FirefoxAmazonWebChromeClient()

        return FirefoxAmazonWebView(context, attrs, client, chromeClient).apply {
            // We experienced crashes if factory init occurs after clients are set.
            factory.initializeWebView(this, 0xFFFFFF, false, null)

            setWebViewClient(client)
            setWebChromeClient(chromeClient)

            initWebview(this)
            initWebSettings(context, settings)
        }
    }
}

private fun initWebview(webView: FirefoxAmazonWebView) = with (webView) {
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = true

    // TODO This does not exist with the AmazonWebView
    //if (BuildConfig.DEBUG) {
    //    setWebContentsDebuggingEnabled(true);
    //}

    // onFocusChangeListener isn't called for AmazonWebView (unlike Android's WebView)
    // so we use the global listener instead.
    viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus: View?, newFocus: View? ->
        if (!viewTreeObserver.isAlive) return@addOnGlobalFocusChangeListener

        // These can both be false if the WebView is not involved in this transaction.
        val isLosingFocus = webView.hasChild(oldFocus)
        val isGainingFocus = webView.hasChild(newFocus)

        // From a user's perspective, the WebView receives focus. Under the hood,
        // the AmazonWebView's child, *Delegate, is actually receiving focus.
        //
        // For why we're doing this, see FocusedDOMElementCache.
        if (isLosingFocus) {
            // Any views (like BrowserNavigationOverlay) that may clear the cache, e.g. by
            // reloading the page, are required to handle their own caching. Here we'll handle
            // cases where the page cache isn't cleared.
            focusedDOMElement.cache()
        } else if (isGainingFocus) {
            // Trying to restore immediately doesn't work - perhaps the WebView hasn't actually
            // received focus yet? Posting to the end of the UI queue seems to solve the problem.
            uiHandler.post { focusedDOMElement.restore() }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled") // We explicitly want to enable JavaScript
@Suppress("DEPRECATION") // To be safe, we'll use delete methods as long as they're there.
private fun initWebSettings(context: Context, settings: AmazonWebSettings) = with (settings) {
    val appName = context.resources.getString(R.string.useragent_appname)
    userAgentString = UserAgent.buildUserAgentString(context, settings, appName)

    javaScriptEnabled = true
    domStorageEnabled = true

    // The default for those settings should be "false" - But we want to be explicit.
    setAppCacheEnabled(false)
    databaseEnabled = false
    javaScriptCanOpenWindowsAutomatically = false

    saveFormData = false
    savePassword = false

    setGeolocationEnabled(false) // We do not implement the callbacks

    builtInZoomControls = true
    displayZoomControls = false // Hide by default

    loadWithOverviewMode = true // To respect the html viewport

    // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
    // Chrome does this in the current Chrome Dev, but not Chrome release).
    // TODO #33: TEXT_AUTOSIZING does not exist in AmazonWebSettings
    //settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

    // Disable access to arbitrary local files by webpages - assets can still be loaded
    // via file:///android_asset/res, so at least error page images won't be blocked.
    allowFileAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false

    // Right now I do not know why we should allow loading content from a content provider
    allowContentAccess = false
}
