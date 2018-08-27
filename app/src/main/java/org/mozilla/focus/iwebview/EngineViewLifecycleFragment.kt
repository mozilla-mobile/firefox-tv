/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.iwebview

import android.os.Bundle
import android.support.annotation.UiThread
import android.view.View
import android.webkit.WebView
import mozilla.components.concept.engine.EngineView
import org.mozilla.focus.R
import org.mozilla.focus.ext.destroy
import org.mozilla.focus.ext.onStart
import org.mozilla.focus.ext.onStop
import org.mozilla.focus.ext.pauseTimers
import org.mozilla.focus.ext.resumeTimers
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.locale.LocaleManager
import java.util.Locale

/**
 * Initializes and manages the lifecycle of an [EngineView] instance inflated by the super class.
 * It was originally inspired by Android's WebViewFragment.
 *
 * To use this class, override it with a super-class that inflates a layout with an [EngineView] with
 * @id=webview.
 *
 * Notes on alternative implementations: while composability is generally preferred over
 * inheritance, there are too many entry points to use this with composition (i.e. all lifecycle
 * methods) so it's more error-prone and we stuck with this implementation. Composability was
 * tried in PR #428.
 */
abstract class EngineViewLifecycleFragment : LocaleAwareFragment() {
    /**
     * The [EngineView] in use by this fragment. If the value is non-null, the EngineView is present
     * in the view hierarchy, null otherwise.
     */
    var webView: EngineView? = null
        @UiThread get // On a background thread, it may have been removed from the view hierarchy.
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = (view.findViewById<View>(R.id.webview) as EngineView).apply {
            onWebViewCreated(this)
        }
    }

    open fun onWebViewCreated(webView: EngineView) = Unit

    override fun onPause() {
        webView!!.pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        webView!!.resumeTimers()
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        webView!!.onStop() // internally calls WebView.onPause: see impl for details.
    }

    override fun onStart() {
        super.onStart()
        webView!!.onStart() // internally calls WebView.onResume: see impl for details.
    }

    override fun onDestroy() {
        super.onDestroy()

        // onDestroy may be called when the webView is null. We think it's because Activities
        // that are being restored (with savedInstanceState) will automatically restore the
        // fragment state and if we perform a FragmentTransaction during the onCreate lifecycle, it
        // will tear the restored state back down (i.e. onDestroy is called twice or without
        // onViewCreated): #694.
        //
        // Note: Focus does this null check too.
        if (webView != null) {
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

            val resources = context!!.resources
            val config = resources.configuration
            config.setLocale(currentLocale)

            @Suppress("DEPRECATION") // TODO: This is non-trivial to fix: #850.
            resources.updateConfiguration(config, null)
        }
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See focus-android issue #666.
        val unneeded = WebView(getContext())
        unneeded.destroy()
    }
}
