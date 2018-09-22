/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import mozilla.components.browser.engine.system.SystemEngineView
import org.mozilla.focus.ext.setupForApp

/**
 * Caches a [SystemEngineView], which internally maintains a [WebView].
 *
 * This allows us to maintain [WebView] state when the view would otherwise
 * be destroyed
 */
object WebViewCache {

    @SuppressLint("StaticFieldLeak")
    private var cachedView: SystemEngineView? = null

    fun getWebView(context: Context, attrs: AttributeSet): SystemEngineView {
        fun View?.removeFromParentIfAble() {
            (this?.parent as? ViewGroup)?.removeView(cachedView)
        }
        fun createAndCacheEngineView(): SystemEngineView {
            return SystemEngineView(context, attrs).apply {
                setupForApp(context)
            }.also { cachedView = it }
        }

        cachedView?.removeFromParentIfAble()
        return cachedView ?: createAndCacheEngineView()
    }

    /**
     * After [WebView.destroy] is called that instance will be unusable and most
     * method calls on it will throw exceptions.  It is important that we clear
     * the cache on destroy, so that future requests receive a new instance.
     */
    fun clear() {
        cachedView = null
    }
}
