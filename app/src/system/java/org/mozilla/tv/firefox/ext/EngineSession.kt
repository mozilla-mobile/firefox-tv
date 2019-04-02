/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.content.Context
import mozilla.components.browser.engine.system.NestedWebView
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.concept.engine.EngineSession

/**
 * [AmazonWebView] requires ActivityContext in order to show 4K resolution rendering option (#277)
 *
 * By default, a-c [SystemEngineSession.webView] uses ApplicationContext. This allows us to
 * override the webView instance
 */
fun EngineSession.resetView(context: Context) {
    (this as SystemEngineSession).webView = NestedWebView(context)
}
