/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webview

import android.content.Context
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.amazon.android.webkit.AmazonWebChromeClient
import com.amazon.android.webkit.AmazonWebKitFactory
import com.amazon.android.webkit.AmazonWebView

import org.mozilla.focus.ext.*
import org.mozilla.focus.session.Session
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.IWebView

import java.util.HashMap

internal class FirefoxAmazonWebView(context: Context, attrs: AttributeSet, factory: AmazonWebKitFactory) : NestedWebView(context, attrs), IWebView {

    @get:VisibleForTesting
    override var callback: IWebView.Callback? = null
        set(callback) {
            field = callback
            client.setCallback(callback)
            chromeClient.callback = callback
            linkHandler.setCallback(callback)
        }

    private val client: FocusWebViewClient
    private val chromeClient = FirefoxAmazonWebChromeClient()
    private val linkHandler: LinkHandler

    init {
        // I think you need to initialize with the factory before initializing the client.
        factory.initializeWebView(this, 0xFFFFFF, false, null)

        client = FocusWebViewClient(getContext().applicationContext)

        setWebViewClient(client)
        setWebChromeClient(chromeClient)

        // TODO This does not exist with the AmazonWebView
        //        if (BuildConfig.DEBUG) {
        //            setWebContentsDebuggingEnabled(true);
        //        }

        isLongClickable = true

        linkHandler = LinkHandler(this)
        setOnLongClickListener(linkHandler)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val connection = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = outAttrs.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING
        return connection
    }

    override fun restoreWebViewState(session: Session) {
        val stateData = session.webViewState

        val backForwardList = if (stateData != null)
            super.restoreState(stateData)
        else
            null

        val desiredURL = session.url.value

        client.restoreState(stateData)
        client.notifyCurrentURL(desiredURL)

        // Pages are only added to the back/forward list when loading finishes. If a new page is
        // loading when the Activity is paused/killed, then that page won't be in the list,
        // and needs to be restored separately to the history list. We detect this by checking
        // whether the last fully loaded page (getCurrentItem()) matches the last page that the
        // WebView was actively loading (which was retrieved during onSaveInstanceState():
        // WebView.getUrl() always returns the currently loading or loaded page).
        // If the app is paused/killed before the initial page finished loading, then the entire
        // list will be null - so we need to additionally check whether the list even exists.

        if (backForwardList != null && backForwardList.currentItem.url == desiredURL) {
            // restoreState doesn't actually load the current page, it just restores navigation history,
            // so we also need to explicitly reload in this case:
            reload()
        } else {
            loadUrl(desiredURL!!)
        }
    }

    override fun saveWebViewState(session: Session) {
        // We store the actual state into another bundle that we will keep in memory as long as this
        // browsing session is active. The data that WebView stores in this bundle is too large for
        // Android to save and restore as part of the state bundle.
        val stateData = Bundle()

        super.saveState(stateData)
        client.saveState(this, stateData)

        session.saveWebViewState(stateData)
    }

    override fun setBlockingEnabled(enabled: Boolean) {
        client.isBlockingEnabled = enabled

        if (this.callback != null) {
            this.callback!!.onBlockingStateChanged(enabled)
        }
    }

    override fun loadUrl(url: String) {
        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        if (!client.shouldOverrideUrlLoading(this, url)) {
            val additionalHeaders = HashMap<String, String>()
            additionalHeaders["X-Requested-With"] = ""

            super.loadUrl(url, additionalHeaders)
        }

        client.notifyCurrentURL(url)
    }

    override fun cleanup() {
        this.deleteData()
    }
}

// todo: move into WebClients file with FocusWebViewClient.
private class FirefoxAmazonWebChromeClient : AmazonWebChromeClient() {
    var callback: IWebView.Callback? = null

    override fun onProgressChanged(view: AmazonWebView?, newProgress: Int) {
        callback?.let { callback ->
            // This is the earliest point where we might be able to confirm a redirected
            // URL: we don't necessarily get a shouldInterceptRequest() after a redirect,
            // so we can only check the updated url in onProgressChanges(), or in onPageFinished()
            // (which is even later).
            val viewURL = view!!.url
            if (!UrlUtils.isInternalErrorURL(viewURL) && viewURL != null) {
                callback.onURLChanged(viewURL)
            }
            callback.onProgress(newProgress)
        }
    }

    override fun onShowCustomView(view: View?, webviewCallback: AmazonWebChromeClient.CustomViewCallback?) {
        val fullscreenCallback = object : IWebView.FullscreenCallback {
            override fun fullScreenExited() {
                webviewCallback!!.onCustomViewHidden()
            }
        }

        callback?.onEnterFullScreen(fullscreenCallback, view)
    }

    override fun onHideCustomView() {
        callback?.onExitFullScreen()
    }
}
