/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.os.Bundle
import android.support.annotation.UiThread
import android.view.View
import android.webkit.WebView
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.locale.LocaleManager
import org.mozilla.focus.session.Session
import java.util.Locale

/**
 * Initializes and manages the lifecycle of an IWebView instance inflated by the super class.
 * It was originally inspired by Android's WebViewFragment.
 *
 * To use this class, override it with a super-class that inflates a layout with an IWebView with
 * @id=webview. Be sure to follow the additional initialization requirements on the [onViewCreated]
 * kdoc.
 *
 * Notes on alternative implementations: while composability is generally preferred over
 * inheritance, there are too many entry points to use this with composition (i.e. all lifecycle
 * methods) so it's more error-prone and we stuck with this implementation. Composability was
 * tried in PR #428.
 */
abstract class IWebViewLifecycleFragment : LocaleAwareFragment() {

    /** Get the initial URL to load after the view has been created. */
    abstract val initialUrl: String
    abstract val session: Session
    abstract val iWebViewCallback: IWebView.Callback

    /**
     * The [IWebView] in use by this fragment. If the value is non-null, the WebView is present
     * in the view hierarchy, null otherwise.
     */
    var webView: IWebView? = null
        @UiThread get // On a background thread, it may have been removed from the view hierarchy.
        private set

    /**
     * Initializes the WebView. By the time this method is called, [session], [initialUrl],
     * and [iWebViewCallback] are expected to be initialized.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = (view.findViewById<View>(R.id.webview) as IWebView).apply {
            callback = iWebViewCallback
            setBlockingEnabled(session.isBlockingEnabled)
            restoreWebViewOrLoadInitialUrl(this)
        }
    }

    private fun restoreWebViewOrLoadInitialUrl(webView: IWebView) {
        if (session.hasWebViewState()) {
            webView.restoreWebViewState(session)
        } else if (!initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl)
        }
    }

    override fun onPause() {
        webView!!.saveWebViewState(session)
        webView!!.onPause()

        super.onPause()
    }

    override fun onResume() {
        webView!!.onResume()

        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (webView != null) {
            webView!!.callback = null
            webView!!.destroy()
            webView = null
        }
    }

    override fun applyLocale() {
        val context = context
        val localeManager = LocaleManager.getInstance()
        if (!localeManager.isMirroringSystemLocale(context)) {
            val currentLocale = localeManager.getCurrentLocale(context)
            Locale.setDefault(currentLocale)

            val resources = context.resources
            val config = resources.configuration
            config.setLocale(currentLocale)
            resources.updateConfiguration(config, null)
        }
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See focus-android issue #666.
        val unneeded = WebView(getContext())
        unneeded.destroy()
    }
}
