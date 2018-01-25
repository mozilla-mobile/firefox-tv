/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.amazon.android.webkit.AmazonWebKitFactory
import com.amazon.android.webkit.AmazonWebSettings
import com.amazon.android.webkit.AmazonWebView
import org.mozilla.focus.R
import org.mozilla.focus.browser.UserAgent
import org.mozilla.focus.webview.FirefoxAmazonWebView
import org.mozilla.focus.webview.TrackingProtectionWebViewClient

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
        return FirefoxAmazonWebView(context, attrs, factory).apply {
            initWebview(this)
            initWebSettings(context, settings)
        }
    }
}

private fun initWebview(webView: AmazonWebView) = with (webView) {
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = true
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
