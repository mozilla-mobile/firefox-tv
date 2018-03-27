/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.iwebview

private const val CACHE_VAR = "_firefoxForFireTvPreviouslyFocusedElement"
private const val CACHE_JS = "var $CACHE_VAR = document.activeElement;"

class FirefoxAmazonFocusedDOMElementCache(private val webView: IWebView) : FocusedDOMElementCache {

    override fun cache() {
        webView.evalJS(CACHE_JS)
    }

    override fun restore() {
        webView.evalJS("if ($CACHE_VAR) $CACHE_VAR.focus();")
    }
}
