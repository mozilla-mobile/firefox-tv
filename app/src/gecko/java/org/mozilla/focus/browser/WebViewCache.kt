/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import mozilla.components.browser.engine.gecko.GeckoEngineView

/**
 * Caches a [SystemEngineView], which internally maintains a [WebView].
 *
 * This allows us to maintain [WebView] state when the view would otherwise
 * be destroyed
 */
class WebViewCache : LifecycleObserver {

    private var cachedView: GeckoEngineView? = null

    fun getWebView(
        context: Context,
        attrs: AttributeSet,
        initialize: GeckoEngineView.() -> Unit
    ): GeckoEngineView {
        fun View?.removeFromParentIfAble() {
            // If the WebView has already been added to the view hierarchy, we
            // need to remove it from its parent before attempting to add it
            // again. Otherwise an IllegalStateException will be thrown
            (this?.parent as? ViewGroup)?.removeView(cachedView)
        }

        fun createAndCacheEngineView(): GeckoEngineView {
            return GeckoEngineView(context, attrs).apply {
                initialize()
            }.also { cachedView = it }
        }

        cachedView?.removeFromParentIfAble()
        return cachedView ?: createAndCacheEngineView()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        cachedView?.onDestroy()
        cachedView = null
    }
}
