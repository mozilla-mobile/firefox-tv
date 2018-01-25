/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.amazon.android.webkit.AmazonWebKitFactory;
import com.amazon.android.webkit.AmazonWebSettings;
import com.amazon.android.webkit.AmazonWebView;
import org.mozilla.focus.R;
import org.mozilla.focus.browser.UserAgent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.webview.SystemWebView;
import org.mozilla.focus.webview.TrackingProtectionWebViewClient;

/**
 * WebViewProvider for creating a WebView based IWebView implementation.
 */
public class WebViewProvider {
    /**
     * Preload webview data. This allows the webview implementation to load resources and other data
     * it might need, in advance of intialising the view (at which time we are probably wanting to
     * show a website immediately).
     */
    public static void preload(final Context context) {
        TrackingProtectionWebViewClient.triggerPreload(context);
    }

    public static void performCleanup(final Context context) {
        SystemWebView.deleteContentFromKnownLocations(context);
    }

    /**
     * A cleanup that should occur when a new browser session starts. This might be able to be merged with
     * {@link #performCleanup(Context)}, but I didn't want to do it now to avoid unforeseen side effects. We can do this
     * when we rethink our erase strategy: #1472.
     *
     * This function must be called before WebView.loadUrl to avoid erasing current session data.
     */
    public static void performNewBrowserSessionCleanup() {
        /*
        // We run this on the main thread to guarantee it occurs before loadUrl so we don't erase current session data.
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();

        // When left open on erase, some pages, like the google search results, will asynchronously write LocalStorage
        // files to disk after we erase them. To work-around this, we delete this data again when starting a new browser session.
        WebStorage.getInstance().deleteAllData();

        StrictMode.setThreadPolicy(oldPolicy);
        */
    }

    public static View create(Context context, AttributeSet attrs, AmazonWebKitFactory factory) {
        final SystemWebView webkitView = new SystemWebView(context, attrs, factory);
        final AmazonWebSettings settings = webkitView.getSettings();
        setupView(webkitView);
        configureDefaultSettings(context, settings);

        return webkitView;
    }

    private static void setupView(AmazonWebView webView) {
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled") // We explicitly want to enable JavaScript
    private static void configureDefaultSettings(Context context, AmazonWebSettings settings) {
        settings.setJavaScriptEnabled(true);

        // Needs to be enabled to display some HTML5 sites that use local storage
        settings.setDomStorageEnabled(true);

        // Enabling built in zooming shows the controls by default
        settings.setBuiltInZoomControls(true);

        // So we hide the controls after enabling zooming
        settings.setDisplayZoomControls(false);

        // To respect the html viewport:
        settings.setLoadWithOverviewMode(true);

        // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
        // Chrome does this in the current Chrome Dev, but not Chrome release).
        // TODO TEXT_AUTOSIZING does not exist in AmazonWebSettings
        //settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Disable access to arbitrary local files by webpages - assets can still be loaded
        // via file:///android_asset/res, so at least error page images won't be blocked.
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        final String appName = context.getResources().getString(R.string.useragent_appname);
        settings.setUserAgentString(UserAgent.buildUserAgentString(context, settings, appName));

        // Right now I do not know why we should allow loading content from a content provider
        settings.setAllowContentAccess(false);

        // The default for those settings should be "false" - But we want to be explicit.
        settings.setAppCacheEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // We do not implement the callbacks - So let's disable it.
        settings.setGeolocationEnabled(false);

        // We do not want to save any data...
        settings.setSaveFormData(false);
        //noinspection deprecation - This method is deprecated but let's call it in case WebView implementations still obey it.
        settings.setSavePassword(false);
    }
}
