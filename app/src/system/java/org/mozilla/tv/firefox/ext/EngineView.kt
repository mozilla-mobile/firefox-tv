/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.webrender.FocusedDOMElementCache
import org.mozilla.tv.firefox.utils.BuildConstants
import java.util.WeakHashMap

// Extension methods on the EngineView class. This is used for additional features that are not part
// of the upstream browser-engine(-system) component yet.

private val uiHandler = Handler(Looper.getMainLooper())

/**
 * Firefox for Fire TV needs to configure every WebView appropriately.
 */
fun EngineView.setupForApp() {
    // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
    // Chrome does this in the current Chrome Dev, but not Chrome release).
    // TODO #33: TEXT_AUTOSIZING does not exist in AmazonWebSettings
    // webView.settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

    if (BuildConstants.isDevBuild) {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    // WebView can be null temporarily after clearData(); however, activity.recreate() would
    // instantiate a new WebView instance
    webView?.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            // For why we're modifying the focusedDOMElement, see FocusedDOMElementCacheInterface.
            //
            // Any views (like BrowserNavigationOverlay) that may clear the cache, e.g. by
            // reloading the page, are required to handle their own caching. Here we'll handle
            // cases where the page cache isn't cleared.
            focusedDOMElement.cache()
        } else {
            // Trying to restore immediately doesn't work - perhaps the WebView hasn't actually
            // received focus yet? Posting to the end of the UI queue seems to solve the problem.
            uiHandler.post { focusedDOMElement.restore() }
        }
    }
}

/**
 * For certain functionality Firefox for Fire TV needs to inject JavaScript into the web content. The engine component
 * does not have such an API yet. It's questionable whether the component will get this raw API as GeckoView doesn't
 * offer a matching API (WebExtensions are likely going to be the preferred way). We may move the functionality that
 * requires JS injection to browser-engine-system.
 */
fun EngineView.evalJS(javascript: String, callback: ValueCallback<String>? = null) {
    webView?.evaluateJavascript(javascript, callback)
}

/**
 * This functionality is not supported by browser-engine-system yet. See [EngineView.evalJS] comment for details.
 */
@SuppressLint("JavascriptInterface")
fun EngineView.addJavascriptInterface(obj: Any, name: String) {
    webView?.addJavascriptInterface(obj, name)
}

/**
 * This functionality is not supported by browser-engine-system yet. See [EngineView.evalJS] comment for details.
 */
fun EngineView.removeJavascriptInterface(interfaceName: String) {
    webView?.removeJavascriptInterface(interfaceName)
}

fun EngineView.scrollByClamped(vx: Int, vy: Int) {
    webView?.apply {
        fun clampScroll(scroll: Int, canScroll: (direction: Int) -> Boolean) = if (scroll != 0 && canScroll(scroll)) {
            scroll
        } else {
            0
        }

        // This is not a true clamp: it can only stop us from
        // continuing to scroll if we've already overscrolled.
        val scrollX = clampScroll(vx) { canScrollHorizontally(it) }
        val scrollY = clampScroll(vy) { canScrollVertically(it) }

        scrollBy(scrollX, scrollY)
    }
}

val EngineView.focusedDOMElement: FocusedDOMElementCache
    get() = getOrPutExtension(this).domElementCache

fun EngineView.saveState(): Bundle {
    val bundle = Bundle()
    getOrPutExtension(this).webView?.saveState(bundle)
    return bundle
}

fun EngineView.restoreState(state: Bundle) {
    getOrPutExtension(this).webView?.restoreState(state)
}

fun EngineView.canGoBackTwice(): Boolean {
    return getOrPutExtension(this).webView?.canGoBackOrForward(-2) ?: false
}

fun EngineView.onPauseIfNotNull() {
    if (webView != null)
        this.onPause()
}

fun EngineView.onResumeIfNotNull() {
    if (webView != null)
        this.onResume()
}

// This method is only for adding extension methods here (as a workaround). Do not expose WebView to the app.
private val EngineView.webView: WebView?
    get() = getOrPutExtension(this).webView

private val extensions = WeakHashMap<EngineView, EngineViewExtension>()

private fun getOrPutExtension(engineView: EngineView): EngineViewExtension {
    extensions[engineView]?.let { return it }

    return EngineViewExtension(engineView).also {
        extensions.clear()
        extensions[engineView] = it
    }
}

/**
 * Cache of additional properties on [EngineView].
 */
private class EngineViewExtension(private val engineView: EngineView) {
    val domElementCache: FocusedDOMElementCache = FocusedDOMElementCache(engineView)

    private val sessionManager: SessionManager = engineView.asView().context.webRenderComponents.sessionManager

    /**
     * Extract the wrapped WebView from the EngineSession. This is a temporary workaround until all required functionality has
     * been implemented in the upstream component.
     */
    val webView: WebView?
        get() =
            if (sessionManager.size > 0) {
                (sessionManager.getOrCreateEngineSession() as SystemEngineSession).webView
            } else {
                // After clearing all session we temporarily don't have a selected session
                // and [SessionRepo.clear()] destroyed the existing webView - see [SystemEngineView.onDestroy()]
                null
            }
}
