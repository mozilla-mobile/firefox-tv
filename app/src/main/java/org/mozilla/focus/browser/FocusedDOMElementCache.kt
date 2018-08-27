/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import mozilla.components.concept.engine.EngineView
import org.mozilla.focus.ext.evalJS
import org.mozilla.focus.iwebview.FocusedDOMElementCache

private const val CACHE_VAR = "_firefoxForFireTvPreviouslyFocusedElement"
private const val CACHE_JS = "var $CACHE_VAR = document.activeElement;"

class FocusedDOMElementCache(private val webView: EngineView) :
    FocusedDOMElementCache {

    override fun cache() {
        webView.evalJS(CACHE_JS)
    }

    override fun restore() {
        webView.evalJS("if ($CACHE_VAR) $CACHE_VAR.focus();")
    }
}
