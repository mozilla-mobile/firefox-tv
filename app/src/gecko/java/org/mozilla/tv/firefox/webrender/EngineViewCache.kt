/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import mozilla.components.browser.engine.gecko.GeckoEngineView
import org.mozilla.tv.firefox.ext.canGoBackTwice
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.session.SessionRepo

/**
 * Caches a [GeckoEngineView], which internally maintains a [WebView].
 *
 * This allows us to maintain [WebView] state when the view would otherwise
 * be destroyed
 */
class EngineViewCache(private val sessionRepo: SessionRepo) : LifecycleObserver {

    companion object {
        // According to Android docs, WebView.saveState and WebView.restoreState do "not restore
        // display data"[1] (the exact meaning of this is not specified). Some discussion about
        // them online implies that they do not behave as expected[2][3], and that previous
        // versions had broken implementations[4]. However, we ship to a limited number of devices,
        // and after thorough testing on each these methods have been found to restore state
        // relatively well.
        //
        // If we encounter strange state bugs in the WebView, this code should be considered
        // suspect. But until then, it solves some very important problems[5][6].
        //
        // [1] https://developer.android.com/reference/android/webkit/WebView.html?hl=es#restoreState(android.os.Bundle)
        // [2] https://stackoverflow.com/a/32867602
        // [3] https://stackoverflow.com/a/33326970
        // [4] https://stackoverflow.com/a/17543769
        // [5] https://github.com/mozilla-mobile/firefox-tv/issues/1276
        // [6] https://github.com/mozilla-mobile/firefox-tv/issues/1256
        private var state: Bundle? = null
    }

    private var cachedView: GeckoEngineView? = null
    private var shouldPersist = true

    fun getEngineView(
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
            // This will need to be updated for GeckoView.
            val engineView = context.webRenderComponents.engine.createView(context, attrs) as GeckoEngineView
            return engineView.apply {
                initialize()
            }.also {
                cachedView = it
            }
        }

        cachedView?.removeFromParentIfAble()
        sessionRepo.canGoBackTwice = { cachedView?.canGoBackTwice() }
        return cachedView ?: createAndCacheEngineView()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        shouldPersist = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        clear()
    }

    private fun clear() {
        sessionRepo.canGoBackTwice = null
        cachedView?.onStop()
        cachedView?.onDestroy()
        cachedView = null
    }

    fun doNotPersist() {
        shouldPersist = false
    }
}
