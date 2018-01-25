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
import org.mozilla.focus.webview.SystemWebView
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
    fun performCleanup(context: Context) {
        SystemWebView.deleteContentFromKnownLocations(context)
    }

    @JvmStatic
    fun create(context: Context, attrs: AttributeSet, factory: AmazonWebKitFactory): View {
        return SystemWebView(context, attrs, factory).apply {
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
private fun initWebSettings(context: Context, settings: AmazonWebSettings) {
    settings.javaScriptEnabled = true

    // Needs to be enabled to display some HTML5 sites that use local storage
    settings.domStorageEnabled = true

    // Enabling built in zooming shows the controls by default
    settings.builtInZoomControls = true

    // So we hide the controls after enabling zooming
    settings.displayZoomControls = false

    // To respect the html viewport:
    settings.loadWithOverviewMode = true

    // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
    // Chrome does this in the current Chrome Dev, but not Chrome release).
    // TODO TEXT_AUTOSIZING does not exist in AmazonWebSettings
    //settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

    // Disable access to arbitrary local files by webpages - assets can still be loaded
    // via file:///android_asset/res, so at least error page images won't be blocked.
    settings.allowFileAccess = false
    settings.allowFileAccessFromFileURLs = false
    settings.allowUniversalAccessFromFileURLs = false

    val appName = context.resources.getString(R.string.useragent_appname)
    settings.userAgentString = UserAgent.buildUserAgentString(context, settings, appName)

    // Right now I do not know why we should allow loading content from a content provider
    settings.allowContentAccess = false

    // The default for those settings should be "false" - But we want to be explicit.
    settings.setAppCacheEnabled(false)
    settings.databaseEnabled = false
    settings.javaScriptCanOpenWindowsAutomatically = false

    // We do not implement the callbacks - So let's disable it.
    settings.setGeolocationEnabled(false)

    // We do not want to save any data...
    settings.saveFormData = false

    settings.savePassword = false
}
