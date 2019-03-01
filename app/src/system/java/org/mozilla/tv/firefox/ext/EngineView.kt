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
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.webrender.FocusedDOMElementCache
import java.util.WeakHashMap

// Extension methods on the EngineView class. This is used for additional features that are not part
// of the upstream browser-engine(-system) component yet.

private val uiHandler = Handler(Looper.getMainLooper())

private const val CACHE_VAR = "_firefoxForFireTvPreviouslyFocusedElement"
private const val CACHE_JS = "var $CACHE_VAR = document.activeElement;"

// This will only happen if YouTube is loading or navigation has broken
private const val noElementFocused = "document.activeElement === null"
// This will only happen if YouTube is loading or navigation has broken
private const val bodyElementFocused = "document.activeElement.tagName === \"BODY\""
private const val sidebarFocused = "document.activeElement.parentElement.parentElement.id === 'guide-list'"

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
fun EngineView.evalJS(javascript: String, callback: ValueCallback<String>? = null) {
    webView?.evaluateJavascript(javascript, callback)
}

fun EngineView.pauseAllVideoPlaybacks() {
    evalJS("document.querySelectorAll('video').forEach(v => v.pause());")
}

fun EngineView.domElementCache() {
    evalJS(CACHE_JS)
}

fun EngineView.domElementRestore() {
    evalJS("if ($CACHE_VAR) $CACHE_VAR.focus();")
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
                    return $noElementFocused ||
                        $bodyElementFocused ||
                        $sidebarFocused;
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

/**
 * This script will:
 * - Add playback state change listeners to all <video>s in the DOM; it uses a mutation
 *   observer to attach listeners to new <video> nodes as well
 * - On playback state change, notify Java about the current playback state
 * - Prevent this script from being injected more than once per page
 *
 * Note that `//` style comments are not supported in `evalJS`.
 *
 * Development tips:
 * - This script was written using Typescript with Visual Studio Code: it may be easier to modify
 *   it by copy-pasting it back-and-forth.
 * - Iterating on the Fire TV is slow: you can speed it up by making this a WebExtension content
 *   script and testing on desktop
 * - For a list of HTMLMediaElement (i.e. video) events, like 'ratechange', see the w3c's HTML5 video
 *   page: https://www.w3.org/2010/05/video/mediaevents.html
 */
private val JS_OBSERVE_PLAYBACK_STATE = """
var _firefoxTV_playbackStateObserverJava;
var _firefoxTV_isPlaybackStateObserverLoaded;
(function () {
    /* seeking will send "pause, play" and so is covered here. */
    const PLAYBACK_STATE_CHANGE_EVENTS = ['play', 'pause', 'ratechange'];
    const MILLIS_BETWEEN_PLAYBACK_STATE_SYNC_BY_TIME = 1000 * 10 /* seconds */;

    const javaInterface = _firefoxTV_playbackStateObserverJava;
    if (!javaInterface) {
        console.error('Cannot sync playback state to Java: JavascriptInterface is not found.');
    }

    const videosWithListeners = new Set();

    let playbackStateSyncIntervalID;

    function onDOMChangedForVideos() {
        addPlaybackStateListeners();
        syncPlaybackState();
    }

    function addPlaybackStateListeners() {
        document.querySelectorAll('video').forEach(videoElement => {
            if (videosWithListeners.has(videoElement)) { return; }
            videosWithListeners.add(videoElement);

            PLAYBACK_STATE_CHANGE_EVENTS.forEach(event => {
                videoElement.addEventListener(event, syncPlaybackState);
            });
        });
    }

    function syncPlaybackState() {
        let isVideoPresent;
        let isPlaying;
        let positionSeconds;
        let playbackRate; /* 0.5, 1, etc. */
        const maybeTargetVideo = getPlayingVideoOrFirstInDOMOrNull();
        if (maybeTargetVideo) {
            isVideoPresent = true;
            isPlaying = !maybeTargetVideo.paused;
            positionSeconds = maybeTargetVideo.currentTime;
            playbackRate = maybeTargetVideo.playbackRate;
        } else {
            isVideoPresent = false;
            isPlaying = false;
            positionSeconds = null;
            playbackRate = null;
        }

        schedulePlaybackStateSyncInterval(isPlaying);

        javaInterface.syncPlaybackState(isVideoPresent, isPlaying, positionSeconds, playbackRate);
    }

    /**
     * When a video is playing, schedules a function to repeatedly sync the playback state;
     * cancels it when there is no video playing.
     *
     * Java and JavaScript increment the current playback position independently and run the risk of
     * getting out of sync (e.g. upon buffering). We could try to handle the buffering case specifically
     * but its state is difficult to identify with and syncing periodically is a better general solution.
     * We don't sync with the video's 'timeupdate' event because it's called very frequently and could
     * detract from performance.
     */
    function schedulePlaybackStateSyncInterval(isVideoPlaying) {
        if (isVideoPlaying && !playbackStateSyncIntervalID) {
            playbackStateSyncIntervalID = setInterval(syncPlaybackState,
                MILLIS_BETWEEN_PLAYBACK_STATE_SYNC_BY_TIME);

        } else if (!isVideoPlaying && playbackStateSyncIntervalID) {
            clearInterval(playbackStateSyncIntervalID);
            playbackStateSyncIntervalID = null;
        }
    }

    function getPlayingVideoOrFirstInDOMOrNull() {
        const maybePlayingVideo = Array.from(document.querySelectorAll('video')).find(video => !video.paused);
        if (maybePlayingVideo) { return maybePlayingVideo; }

        /* If there are no playing videos, just return the first one. */
        return document.querySelector('video');
    }

    function nodeContainsVideo(node) {
        return node.nodeName.toLowerCase() === 'video' ||
                ((node instanceof Element) && !!node.querySelector('video'));
    }

    const documentChangedObserver = new MutationObserver(mutationList => {
        const wasVideoAdded = mutationList.some(mutation => {
            return mutation.type === 'childList' &&
                    (Array.from(mutation.addedNodes).some(nodeContainsVideo) ||
                            Array.from(mutation.removedNodes).some(nodeContainsVideo));
        });

        if (wasVideoAdded) {
            /* This may traverse the whole DOM so let's only call it if it's necessary. */
            onDOMChangedForVideos();
        }
    });

    /* Sometimes the script is evaluated more than once per page:
     * only inject code with side effects once. */
    if (!_firefoxTV_isPlaybackStateObserverLoaded) {
        _firefoxTV_isPlaybackStateObserverLoaded = true;

        documentChangedObserver.observe(document, {subtree: true, childList: true});

        /* The DOM is changed from blank to filled for the initial page load.
         * While the function name assumes videos are present, checking for
         * videos is as expensive as calling the function so we just call it. */
        onDOMChangedForVideos();
    }
})();
""".trimIndent()
