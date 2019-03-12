/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.content.Context
import mozilla.components.browser.engine.system.NestedWebView
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineSession

/**
 * [AmazonWebView] requires ActivityContext in order to show 4K resolution rendering option (#277)
 *
 * By default, a-c [SystemEngineSession.webView] uses ApplicationContext. This allows us to
 * override the webView instance
 */
fun EngineSession.resetView(context: Context, session: Session? = null) {
    (this as SystemEngineSession).webView = NestedWebView(context)

    /**
     * When calling getOrCreateEngineSession(), [SessionManager] lazily creates an [EngineSession]
     * instance and links it with its respective [Session]. During the linking, [SessionManager]
     * calls EngineSession.loadUrl(session.url), which, during initialization, is Session.initialUrl
     *
     * This is how "about:home" successfully gets added to [WebView.WebForwardList], with which
     * we do various different operations (such as exiting the app and handling Youtube back)
     *
     * We need to manually reload the session.url since we are replacing the webview instance that
     * has already called loadUrl(session.url) during [EngineView] lazy instantiation
     */
    session?.let {
        this.loadUrl(it.url)
    }
}
