/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.engine.EngineView
import org.mozilla.geckoview.GeckoView
import org.mozilla.tv.firefox.ext.Js.CACHE_JS
import org.mozilla.tv.firefox.ext.Js.BODY_ELEMENT_FOCUSED
import org.mozilla.tv.firefox.ext.Js.JS_OBSERVE_PLAYBACK_STATE
import org.mozilla.tv.firefox.ext.Js.NO_ELEMENT_FOCUSED
import org.mozilla.tv.firefox.ext.Js.PAUSE_VIDEO
import org.mozilla.tv.firefox.ext.Js.RESTORE_JS
import org.mozilla.tv.firefox.ext.Js.SIDEBAR_FOCUSED
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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun EngineView.evalJS(javascript: String, callback: ValueCallback<String>? = null) {
    geckoView?.session?.loadUri("javascript:$javascript")
    callback.forceExhaustive
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

/** TODO: Replace stub when functionality is available in GV (See #1837) */
fun EngineView.handleYoutubeBack(@Suppress("UNUSED_PARAMETER") indexToGoBackTo: Int) { }
object WebHistory {
    val currentIndex: Int
        get() = 0
}

/** TODO: Replace stub when functionality is available in GV (See #1837) */
val EngineView.backForwardList: WebHistory
    get() = WebHistory

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

/** TODO: Replace stub when functionality is available in GV */
fun EngineView.canGoBackTwice(): Boolean {
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
