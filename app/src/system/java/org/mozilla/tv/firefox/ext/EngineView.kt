/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebBackForwardList
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.ext.Js.BODY_ELEMENT_FOCUSED
import org.mozilla.tv.firefox.ext.Js.CACHE_JS
import org.mozilla.tv.firefox.ext.Js.JS_OBSERVE_PLAYBACK_STATE
import org.mozilla.tv.firefox.ext.Js.NO_ELEMENT_FOCUSED
import org.mozilla.tv.firefox.ext.Js.PAUSE_VIDEO
import org.mozilla.tv.firefox.ext.Js.RESTORE_JS
import org.mozilla.tv.firefox.ext.Js.SIDEBAR_FOCUSED
import org.mozilla.tv.firefox.webrender.FocusedDOMElementCache
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

    // WebView can be null temporarily after clearData(); however, activity.recreate() would
    // instantiate a new WebView instance
    webView?.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            // For why we're modifying the focusedDOMElement, see FocusedDOMElementCacheInterface.
            //
            // This will cache focus whenever the app is backgrounded or the device goes to sleep,
            // as well as whenever another view takes focus.
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
 * does not have such an API yet. It's questionable whether the component will get this raw API as WebView doesn't
 * offer a matching API (WebExtensions are likely going to be the preferred way). We may move the functionality that
 * requires JS injection to browser-engine-system.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun EngineView.evalJS(javascript: String, callback: ValueCallback<String>? = null) {
    webView?.evaluateJavascript(javascript, callback)
}

fun EngineView.pauseAllVideoPlaybacks() {
    evalJS(PAUSE_VIDEO)
}

fun EngineView.cacheDomElement() {
    evalJS(CACHE_JS)
}

fun EngineView.restoreDomElement() {
    evalJS(RESTORE_JS)
}

fun EngineView.observePlaybackState() {
    evalJS(JS_OBSERVE_PLAYBACK_STATE)
}

private fun EngineView.evalJSWithTargetVideo(getExpressionToEval: (videoId: String) -> String) {
    val ID_TARGET_VIDEO = "targetVideo"
    val GET_TARGET_VIDEO_OR_RETURN = """
            |var videos = Array.from(document.querySelectorAll('video'));
            |if (videos.length === 0) { return; }
            |
            |var $ID_TARGET_VIDEO = videos.find(function (video) { return !video.paused });
            |if (!$ID_TARGET_VIDEO) {
            |    $ID_TARGET_VIDEO = videos[0];
            |}
            """.trimMargin()

    val expressionToEval = getExpressionToEval(ID_TARGET_VIDEO)
    evalJS("""
            |(function() {
            |    $GET_TARGET_VIDEO_OR_RETURN
            |    $expressionToEval
            |})();
            """.trimMargin())
}

fun EngineView.playTargetVideo() {
    evalJSWithTargetVideo { videoId -> "$videoId.play();" }
}

fun EngineView.pauseTargetVideo(isInterruptedByVoiceCommand: Boolean) {
    fun getJS(videoId: String) = if (!isInterruptedByVoiceCommand) {
        "$videoId.pause();"
    } else {
        // The video is paused for us during a voice command: my theory is that WebView
        // pauses/resumes videos when audio focus is revoked/granted to it (while it's given
        // to the voice command). Unfortunately, afaict there is no way to prevent WebView
        // from resuming these paused videos so we have to pause it after it resumes.
        // Unfortunately, there is no callback for this (or audio focus changes) so we
        // inject JS to pause the video immediately after it starts again.
        //
        // We timeout the if-playing-starts-pause listener so, if for some reason this
        // listener isn't called immediately, it doesn't pause the video after the user
        // attempts to play it in the future (e.g. user says "pause" while video is already
        // paused and then requests a play).
        """
                    | var playingEvent = 'playing';
                    | var initialExecuteMillis = new Date();
                    |
                    | function onPlay() {
                    |     var now = new Date();
                    |     var millisPassed = now.getTime() - initialExecuteMillis.getTime();
                    |     if (millisPassed < 1000) {
                    |         $videoId.pause();
                    |     }
                    |
                    |     $videoId.removeEventListener(playingEvent, onPlay);
                    | }
                    |
                    | $videoId.addEventListener(playingEvent, onPlay);
                """.trimMargin()
    }

    evalJSWithTargetVideo(::getJS)
}

fun EngineView.seekTargetVideoToPosition(absolutePositionSeconds: Long) {
    evalJSWithTargetVideo { videoId -> "$videoId.currentTime = $absolutePositionSeconds;" }
}

fun EngineView.checkYoutubeBack(callback: ValueCallback<String>) {
    val shouldWeExitPage = """
               (function () {
                    return $NO_ELEMENT_FOCUSED ||
                        $BODY_ELEMENT_FOCUSED ||
                        $SIDEBAR_FOCUSED;
                })();
        """.trimIndent()

    evalJS(shouldWeExitPage, callback)
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

fun EngineView.handleYoutubeBack(indexToGoBackTo: Int) {
    val goBackSteps = backForwardList.currentIndex - indexToGoBackTo
    webView!!.goBackOrForward(-goBackSteps)
}

val EngineView.backForwardList: WebBackForwardList
        get() = webView!!.copyBackForwardList()

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
                // and [SessionRepo.clear()] destroyed the existing webview - see [SystemEngineView.onDestroy()]
                null
            }
}
