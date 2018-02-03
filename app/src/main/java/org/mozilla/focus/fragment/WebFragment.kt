/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.locale.LocaleManager
import org.mozilla.focus.session.Session
import org.mozilla.focus.web.IWebView
import java.util.Locale

/**
 * Base implementation for fragments that use an IWebView instance. Based on Android's WebViewFragment.
 */
abstract class WebFragment : LocaleAwareFragment() {
    var webView: IWebView? = null
        get() = if (isWebViewAvailable) field else null
    private var isWebViewAvailable = false

    /** Get the initial URL to load after the view has been created. */
    abstract val initialUrl: String
    abstract val session: Session

    abstract fun createCallback(): IWebView.Callback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = (view.findViewById<View>(R.id.webview) as IWebView).apply {
            callback = createCallback()
            setBlockingEnabled(session.isBlockingEnabled)
            restoreWebViewOrLoadInitialUrl(this)
        }
        isWebViewAvailable = true
    }

    private fun restoreWebViewOrLoadInitialUrl(webView: IWebView) {
        if (session.hasWebViewState()) {
            webView.restoreWebViewState(session)
        } else if (!initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl)
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

    override fun onPause() {
        webView!!.saveWebViewState(session)
        webView!!.onPause()

        super.onPause()
    }

    override fun onResume() {
        webView!!.onResume()

        super.onResume()
    }

    override fun onDestroy() {
        if (webView != null) {
            webView!!.callback = null
            webView!!.destroy()
            webView = null
        }

        super.onDestroy()
    }

    override fun onDestroyView() {
        isWebViewAvailable = false

        super.onDestroyView()
    }
}
