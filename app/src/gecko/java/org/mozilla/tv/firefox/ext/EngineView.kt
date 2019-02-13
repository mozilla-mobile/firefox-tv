/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.widget.FrameLayout
import mozilla.components.concept.engine.EngineView
import org.mozilla.geckoview.GeckoView
import org.mozilla.tv.firefox.webrender.FocusedDOMElementCache
import java.util.WeakHashMap

// Extension methods on the EngineView class. This is used for additional features that are not part
// of the upstream browser-engine(-gecko) component yet.

private val uiHandler = Handler(Looper.getMainLooper())

/**
 * Firefox for Fire TV needs to configure every WebView appropriately.
 */
fun EngineView.setupForApp() {
    geckoView?.setOnFocusChangeListener { _, hasFocus ->
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

fun EngineView.evalJS(javascript: String, callback: ValueCallback<String>? = null) {
    geckoView?.session?.loadUri("javascript:$javascript")
    callback.forceExhaustive
}

fun EngineView.pauseAllVideoPlaybacks() {
    evalJS("document.querySelectorAll('video').forEach(v => v.pause());")
}

/**
 * This functionality is not supported by browser-engine-gecko yet. See [EngineView.evalJS] comment for details.
 */
fun EngineView.addJavascriptInterface(obj: Any, name: String) {
    println("TODO: require media interface from platform team $obj $name")
}

fun EngineView.removeJavascriptInterface(interfaceName: String) {
    println("TODO: require media interface from platform team $interfaceName")
}

fun EngineView.scrollByClamped(vx: Int, vy: Int) {
    geckoView?.apply {
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

/** TODO: Investigate a way to handle exiting YouTube with hardware back in GV (See #1837) */
fun EngineView.handleYoutubeBack(@Suppress("UNUSED_PARAMETER") indexToGoBackTo: Int) {
//    val goBackSteps = backForwardList.currentIndex - indexToGoBackTo
//    geckoView!!.goBackOrForward(-goBackSteps)
}
object WebHistory {
    val currentIndex: Int
        get() = 0
}

val EngineView.backForwardList: WebHistory
    get() = WebHistory
//  get() = geckoView!!.copyBackForwardList()

val EngineView.focusedDOMElement: FocusedDOMElementCache
    get() = getOrPutExtension(this).domElementCache

// fun EngineView.saveState(): Bundle {
//     val bundle = Bundle()
//     getOrPutExtension(this).geckoView?
//     return bundle
// }
//
// fun EngineView.restoreState(state: Bundle) {
//     getOrPutExtension(this).geckoView?.restoreState(state)
// }

fun EngineView.canGoBackTwice(): Boolean {
//    return getOrPutExtension(this).geckoView?.canGoBackOrForward(-2) ?: false
    return false
}

fun EngineView.onPauseIfNotNull() {
    if (geckoView != null)
        this.onPause()
}

fun EngineView.onResumeIfNotNull() {
    if (geckoView != null)
        this.onResume()
}

// This method is only for adding extension methods here (as a workaround). Do not expose WebView to the app.
private val EngineView.geckoView: GeckoView?
    get() = getOrPutExtension(this).geckoView

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

    /**
     * Extract the wrapped WebView from the EngineSession. This is a temporary workaround until all required functionality has
     * been implemented in the upstream component.
     */
    val geckoView: GeckoView?
        get() = (engineView.asView() as FrameLayout).getChildAt(0) as GeckoView
}
