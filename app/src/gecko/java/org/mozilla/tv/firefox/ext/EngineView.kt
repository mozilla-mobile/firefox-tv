/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.os.Handler
import android.os.Looper
import android.support.annotation.VisibleForTesting
import android.webkit.ValueCallback
import android.widget.FrameLayout
import mozilla.components.concept.engine.EngineView
import org.mozilla.geckoview.GeckoView
import org.mozilla.tv.firefox.ext.JS.CACHE_JS
import org.mozilla.tv.firefox.ext.JS.CACHE_VAR
import org.mozilla.tv.firefox.ext.JS.bodyElementFocused
import org.mozilla.tv.firefox.ext.JS.noElementFocused
import org.mozilla.tv.firefox.ext.JS.pauseVideo
import org.mozilla.tv.firefox.ext.JS.sidebarFocused
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
    evalJS(pauseVideo)
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
