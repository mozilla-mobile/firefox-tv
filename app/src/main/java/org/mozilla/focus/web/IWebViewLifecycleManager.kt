/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.mozilla.focus.R
import org.mozilla.focus.session.Session

/**
 * Manages the lifecycle callbacks of an IWebView embedded in a Fragment. The lifecycle
 * state available in the nullability of the [webView] reference: see that for more details.
 *
 * To use this class, in the Fragment intending to use it:
 *   1) Store a reference to this class within a Fragment.
 *   2) Call Fragment.getLifecycle().addObserver on this object.
 *   3) Override onCreateView, inflating a layout with an [IWebView] that has @id="webview". Call
 * [onCreateView] on this object.
 *   4) In Fragment.onCreateView/onDestroyView, call the method of the same name on this object.
 *   5) If locale switching is supported, call [onApplyLocale] in LocaleAwareFragment.onApplyLocale.
 *
 * This requires many steps because Android's LifecycleObserver does not provide Fragment
 * lifecycle callbacks.
 */
@Suppress("UNUSED_PARAMETER") // @OnLifecycleEvent generates `lifecycleOwner` parameters.
class IWebViewLifecycleManager(
        private val session: Session,
        private val callback: IWebView.Callback
) : LifecycleObserver {

    /**
     * The WebView associated with this fragment. If the webview is still active in its lifecycle,
     * this reference will return a non-null value and will return null if it's inactive.
     *
     * This value should only be accessed on the UI thread to ensure it's still attached to
     * the view hierarchy when in use.
     */
    var webView: IWebView? = null
        private set

    fun onCreateView(webviewContainer: ViewGroup, initialUrl: String) {
        val webView = webviewContainer.findViewById<View>(R.id.webview) as? IWebView ?:
                throw IllegalArgumentException("No IWebView, id=webview, available on provided ViewGroup")
        webView.callback = this@IWebViewLifecycleManager.callback
        webView.setBlockingEnabled(session.isBlockingEnabled)
        restoreWebViewOrLoadInitialUrl(webView, initialUrl)
        this.webView = webView
    }

    fun onDestroyView() {
        val webView = webView ?: throw IllegalStateException("this.webview unexpectedly null")
        webView.callback = null
        webView.destroy()
        this.webView = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(lifecycleOwner: LifecycleOwner) {
        val webView = webView ?: throw IllegalStateException("this.webview unexpectedly null")
        webView.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(lifecycleOwner: LifecycleOwner) {
        val webView = webView ?: throw IllegalStateException("this.webview unexpectedly null")
        webView.saveWebViewState(session)
        webView.onPause()
    }

    private fun restoreWebViewOrLoadInitialUrl(webView: IWebView, initialUrl: String) {
        if (session.hasWebViewState()) {
            webView.restoreWebViewState(session)
        } else if (!initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl)
        }
    }

    fun onApplyLocale(context: Context) {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        // TODO: this is only necessary for webview - encapsulate it properly.
        // TODO: this wouldn't work for AmazonWebView but we don't support locale switching yet.
        // TODO: this fn makes LifecycleManager responsible for more than 1 thing: we should move
        // this code elsewhere. Don't forget to update the class javadoc.
        val unneeded = WebView(context);
        unneeded.destroy();
    }
}
