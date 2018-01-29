/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
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
    private var webView: IWebView? = null
    private var isWebViewAvailable: Boolean = false

    abstract val session: Session?

    /**
     * Get the initial URL to load after the view has been created.
     */
    abstract val initialUrl: String?

    /**
     * Inflate a layout for this fragment. The layout needs to contain a view implementing IWebView
     * with the id set to "webview".
     */
    abstract fun inflateLayout(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View

    abstract fun createCallback(): IWebView.Callback

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflateLayout(inflater, container, savedInstanceState)

        webView = view.findViewById<View>(R.id.webview)
        isWebViewAvailable = true
        webView!!.callback = createCallback()

        val session = session

        if (session != null) {
            webView!!.setBlockingEnabled(session.isBlockingEnabled)
        }

        if (session == null || !session.hasWebViewState()) {
            val url = initialUrl
            if (!TextUtils.isEmpty(url)) {
                webView!!.loadUrl(url!!)
            }
        } else {
            webView!!.restoreWebViewState(session)
        }

        return view
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
            context.resources.updateConfiguration(config, null)
        }
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(getContext())
        unneeded.destroy()
    }

    override fun onPause() {
        val session = session
        if (session != null) {
            webView!!.saveWebViewState(session)
        }

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

    fun getWebView(): IWebView? {
        return if (isWebViewAvailable) webView else null
    }
}
