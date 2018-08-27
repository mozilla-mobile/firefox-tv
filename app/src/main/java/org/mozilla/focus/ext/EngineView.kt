/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import android.widget.FrameLayout
import mozilla.components.concept.engine.EngineView
import org.mozilla.focus.R
import org.mozilla.focus.browser.FocusedDOMElementCache
import org.mozilla.focus.browser.UserAgent
import org.mozilla.focus.utils.AppConstants
import java.util.WeakHashMap

// Extension methods on the EngineView class. This is used for additional features that are not part
// of the upstream browser-engine(-system) component yet.

/**
 * Firefox for Fire TV needs to configure every WebView appropriately.
 *
 * We are going to have an API for engine default settings in 0.21 but it doesn't cover all settings used by Firefox
 * for Fire TV yet. As a workaround we extract the actual WebView instance and set it up manually until the engine
 * component is powerful enough.
 */
@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
fun EngineView.setupForApp(context: Context) {
    val webView = extractWebView().apply {
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = true
    }

    webView.settings.apply {
        val appName = context.resources.getString(R.string.useragent_appname)
        userAgentString = UserAgent.buildUserAgentString(context, this, appName)

        javaScriptEnabled = true
        domStorageEnabled = true

        mediaPlaybackRequiresUserGesture = false // Allows auto-play (which improves YouTube experience).

        // The default for those settings should be "false" - But we want to be explicit.
        setAppCacheEnabled(false)
        databaseEnabled = false
        javaScriptCanOpenWindowsAutomatically = false

        saveFormData = false
        savePassword = false

        setGeolocationEnabled(false) // We do not implement the callbacks

        builtInZoomControls = true
        displayZoomControls = false // Hide by default

        loadWithOverviewMode = true // To respect the html viewport

        // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
        // Chrome does this in the current Chrome Dev, but not Chrome release).
        // TODO #33: TEXT_AUTOSIZING does not exist in AmazonWebSettings
        // settings.setLayoutAlgorithm(AmazonWebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Disable access to arbitrary local files by webpages - assets can still be loaded
        // via file:///android_asset/res, so at least error page images won't be blocked.
        allowFileAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false

        // Right now I do not know why we should allow loading content from a content provider
        allowContentAccess = false
    }

    if (AppConstants.isDevBuild()) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

/**
 * For certain functionality Firefox for Fire TV needs to inject JavaScript into the web content. The engine component
 * does not have such an API yet. It's questionable whether the component will get this raw API as GeckoView doesn't
 * offer a matching API (WebExtensions are likely going to be the preferred way). We may move the functionality that
 * requires JS injection to browser-engine-system.
 */
fun EngineView.evalJS(javascript: String) {
    extractWebView().loadUrl("javascript:$javascript")
}

/**
 * This functionality is not supported by browser-engine-system yet. See [EngineView.evalJS] comment for details.
 */
@SuppressLint("JavascriptInterface")
fun EngineView.addJavascriptInterface(obj: Any, name: String) {
    extractWebView().addJavascriptInterface(obj, name)
}

/**
 * This functionality is not supported by browser-engine-system yet. See [EngineView.evalJS] comment for details.
 */
fun EngineView.removeJavascriptInterface(interfaceName: String?) {
    extractWebView().removeJavascriptInterface(interfaceName)
}

/**
 * Creating a screenshot (thumbnail) of the currently visible page.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/495
 */
fun EngineView.takeScreenshot(): Bitmap {
    return extractWebView().let { webView ->
        webView.buildDrawingCache()
        val outBitmap = Bitmap.createBitmap(webView.drawingCache)
        webView.destroyDrawingCache()
        outBitmap
    }
}

fun EngineView.scrollByClamped(vx: Int, vy: Int) {
    extractWebView().apply {
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

/**
 * Forwarding lifecycle method to WebView.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/657
 */
fun EngineView.pauseTimers() = extractWebView().pauseTimers()
fun EngineView.resumeTimers() = extractWebView().resumeTimers()
fun EngineView.onStop() = extractWebView().apply {
    // NB: onStop unexpectedly calls onPause: see below.
    //
    // When the user says "Alexa pause [the video]", the Activity will be paused/resumed while
    // Alexa handles the request. If the WebView is paused during video playback, the video will
    // have poor behavior (on YouTube the screen goes black, may rebuffer, and may lose the voice
    // command). Unfortunately, there does not appear to be any way to prevent this other than
    // to not call WebView.onPause so we pause the WebView later, here in onStop, when it isn't
    // affected by Alexa voice commands. Luckily, Alexa pauses the video for us. afaict, on
    // Fire TV, `onPause` without `onStop` isn't called very often so I don't think there will
    // be many side effects (#936).
    //
    // The problem is not reproducible when onPause is called here, even if pauseTimers is
    // called in onPause.
    onPause()
}
fun EngineView.onStart() = extractWebView().apply {
    // NB: onStart unexpectedly calls onResume: see onStop for details.
    onResume()
}
fun EngineView.destroy() = extractWebView().destroy()

val EngineView.focusedDOMElement: FocusedDOMElementCache
    get() = domElementCache.getOrPut(this) {
        FocusedDOMElementCache(this)
    }

/**
 * Extract the wrapped WebView from the EngineView. This is a temporary workaround until all required functionality has
 * been implemented in the upstream component.
 *
 * For now EngineView wraps a single WebView and we can easily extract that and apply workarounds. Later EngineView may
 * keep multiple WebView instances to animate tab switches. However this part is not implemented yet and we should make
 * sure that we upstream the missing functionality first.
 */
private fun EngineView.extractWebView(): WebView {
    val viewGroup = (asView() as FrameLayout)
    return viewGroup.getChildAt(0) as WebView
}

private val domElementCache: WeakHashMap<EngineView, FocusedDOMElementCache> = WeakHashMap()
