/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.arch.lifecycle.Lifecycle.Event.ON_DESTROY
import android.arch.lifecycle.Lifecycle.Event.ON_START
import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.support.annotation.UiThread
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.webkit.JavascriptInterface
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.browser.VideoVoiceCommandMediaSession.MediaSessionCallbacks
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.session.Session
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

private const val JS_INTERFACE_IDENTIFIER = "_firefoxTV_playbackStateObserverJava"
private const val MEDIA_SESSION_TAG = "FirefoxTVMedia"

private const val SUPPORTED_ACTIONS = ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or
        ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS or
        ACTION_SEEK_TO // "Alexa, rewind/fast-forward <num> <unit-of-time>"

private val KEY_EVENT_ACTIONS_DOWN_UP = listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)

/**
 * An encapsulation of a [MediaSessionCompat] instance to allow voice commands on videos; we
 * handle some hardware keys here too: see [MediaSessionCallbacks].
 *
 * Before use, callers should:
 * - Add this as a [LifecycleObserver]
 * - Add [dispatchKeyEvent] to KeyEvent handling
 * - Call [onCreateWebView] and [onDestroyWebView] for fragment lifecycle handling.
 *
 * To save time, we don't handle audio through either voice or the remote play/pause button: we don't
 * explicitly handle playback changes ourselves and we mute play/pause events from being received
 * by the page (see [dispatchKeyEvent]).
 *
 * When a MediaSession is active, it is available to receive MediaController commands (e.g. Alexa
 * voice commands). MediaSessions begin inactive. They become active when they go to the playing or
 * buffering states. They are inactivated when they go to stopped, none, or error states.
 *
 * For simplicity, we keep our MediaSession active (buffering, playing, paused) while Firefox is
 * in the foreground and deactivate it (stopped) in the background. If we wanted to be more accurate,
 * we could add a state (none) when there are no videos present in the DOM (#955).
 *
 * The constructor should be called from the UiThread because of [mediaSession].
 *
 * [1]: If the initial playback state is PAUSED or NONE, a music selection voice conversation
 * overrides our voice commands.
 */
class VideoVoiceCommandMediaSession @UiThread constructor(
        private val activity: AppCompatActivity
) : LifecycleObserver {

    @get:UiThread // MediaSessionCompat is not thread safe.
    private val mediaSession = MediaSessionCompat(activity, MEDIA_SESSION_TAG)

    /* Since we may update playback state often, we cache this builder to reduce allocation. */
    @get:UiThread // PlaybackStateCompat.Builder is not thread safe.
    private val cachedPlaybackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)

    private var webView: IWebView? = null
    private var sessionIsLoadingObserver: SessionIsLoadingObserver? = null

    private var isStarted = false

    init {
        mediaSession.setCallback(MediaSessionCallbacks())
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
    }

    fun onCreateWebView(webView: IWebView, session: Session) {
        this.webView = webView.apply {
            addJavascriptInterface(JavascriptVideoPlaybackStateSyncer(), JS_INTERFACE_IDENTIFIER)
        }

        val sessionIsLoadingObserver = SessionIsLoadingObserver(webView)
        session.loading.observe(activity, sessionIsLoadingObserver)
        this.sessionIsLoadingObserver = sessionIsLoadingObserver
    }

    fun onDestroyWebView(webView: IWebView, session: Session) {
        webView.removeJavascriptInterface(JS_INTERFACE_IDENTIFIER)
        this.webView = null

        session.loading.removeObserver(sessionIsLoadingObserver!!)
        this.sessionIsLoadingObserver = null
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        isStarted = true

        // We want to make our MediaSession active: state buffering is more accurate than state
        // playing. For an explanation of MediaSession (in)active states, see class javadoc.
        //
        // The state should be synced with the DOM on page load (i.e. the script is injected) or
        // video playback state change (see JS script).
        val playbackState = cachedPlaybackStateBuilder
                .setState(STATE_BUFFERING, PLAYBACK_POSITION_UNKNOWN, 0f)
                .build()
        mediaSession.setPlaybackState(playbackState)
        mediaSession.isActive = true
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        isStarted = false

        // Videos playing when the app was backgrounded get into an inconsistent state: `paused`
        // will return false but they won't actually be playing. This makes any user "play" commands
        // take two clicks: one to "pause" and the second to play for real. We get around this by
        // pausing all videos before being backgrounded. This issue doesn't affect pages with
        // autoplay, like YouTube.
        //
        // The videos may send playback state update events to Java, which we're forced to ignore:
        // see JavascriptVideoPlaybackStateSyncer for the code.
        webView?.evalJS("document.querySelectorAll('video').forEach(v => v.pause());")

        // Move MediaSession to inactive state.
        val playbackState = cachedPlaybackStateBuilder
                .setState(STATE_STOPPED, PLAYBACK_POSITION_UNKNOWN, 0f)
                .build()
        mediaSession.setPlaybackState(playbackState)
        mediaSession.isActive = false
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() {
        mediaSession.release()
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean = when (event.keyCode) {
        // If we did nothing, on key down, hardware media buttons are swallowed and sent to
        // [MediaSessionCallbacks]. On key up, these key events are sent to the WebView and web
        // page. This means the key event would be handled twice - once for each key down and up -
        // which would undo any playback state updates. Thus we mute the WebView key up events and
        // handle it all ourselves through MediaSession.
        KEYCODE_MEDIA_PLAY_PAUSE, KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE -> event.action == KeyEvent.ACTION_UP
        else -> false
    }

    class SessionIsLoadingObserver(private val webView: IWebView) : Observer<Boolean> {
        override fun onChanged(it: Boolean?) {
            val isLoading = it!! // Observer is attached to NonNullLiveData.
            if (!isLoading) {
                webView.evalJS(JS_OBSERVE_PLAYBACK_STATE) // Calls through to JavascriptVideoPlaybackStateSyncer.
            }
        }
    }

    inner class JavascriptVideoPlaybackStateSyncer {

        /**
         * Called by JavaScript to sync playback state to Java's [mediaSession].
         *
         * Note: JavaScript calling into Kotlin does not support optionals.
         */
        @JavascriptInterface
        fun syncPlaybackState(isVideoPresent: Boolean, isVideoPlaying: Boolean,
                              jsPositionSeconds: Double, jsPlaybackSpeed: Float) {
            // During onStop we pause all videos which may send playback state updates to this
            // method. In theory, since the JS is async, this could undo the playback state we set
            // in onStop so we ignore these updates. In practice, this method doesn't appear to be
            // called from that JS but we leave this in for safety.
            if (!isStarted) { return }

            val playbackStateInt: Int
            val positionMillis: Long
            val playbackSpeed: Float
            if (isVideoPresent) {
                playbackStateInt = if (isVideoPlaying) STATE_PLAYING else STATE_PAUSED
                positionMillis = TimeUnit.SECONDS.toMillis(jsPositionSeconds.roundToLong())
                playbackSpeed = if (isVideoPlaying) jsPlaybackSpeed else 0f // setState docs say 0 if paused.
            } else {
                playbackStateInt = STATE_PAUSED // We want to keep session active so used paused.
                positionMillis = PLAYBACK_POSITION_UNKNOWN
                playbackSpeed = 0f
            }

            launch(UI) { // mediaSession and cachedPlaybackState is on UI thread only.
                val playbackState = cachedPlaybackStateBuilder
                        .setState(playbackStateInt, positionMillis, playbackSpeed)
                        .build()
                mediaSession.setPlaybackState(playbackState)
            }
        }
    }

    /**
     * Callbacks for voice commands ("Alexa play") and hardware media buttons on key down. See
     * [dispatchKeyEvent] for more details on hardware media button propagation.
     *
     * These callbacks are expected to update playback state: our code does, but we'll often go
     * through JavaScript first.
     *
     * Due to time constraints, the code is written for a single video on the page,
     * which should cover the majority use case (#973 for multiple videos, #935 for audio).
     */
    inner class MediaSessionCallbacks : MediaSessionCompat.Callback() {
        private val ID_TARGET_VIDEO = "targetVideo"
        private val GET_TARGET_VIDEO_OR_RETURN = """
            |var videos = Array.from(document.querySelectorAll('video'));
            |if (videos.length === 0) { return; }
            |
            |var $ID_TARGET_VIDEO = videos.find(function (video) { return !video.paused });
            |if (!$ID_TARGET_VIDEO) {
            |    $ID_TARGET_VIDEO = videos[0];
            |}
            """.trimMargin()

        // NB: this hasn't been tested on expressions that are more than one line.
        private fun evalJSWithTargetVideo(getExpressionToEval: (videoId: String) -> String) {
            val expressionToEval = getExpressionToEval(ID_TARGET_VIDEO)
            webView?.evalJS("""
                |(function() {
                |    $GET_TARGET_VIDEO_OR_RETURN
                |    $expressionToEval
                |})();
                """.trimMargin())
        }

        /**
         * Called to play media.
         *
         * If a play or pause voice command is issued, the corresponding method
         * will be called. In JS, if `play` is called on an already playing video, it's a no-op.
         *
         * If a play/pause hardware media button (it's one button, not two) is hit, it will send
         * either [onPlay] or [onPause] as a toggle, based on current MediaSession playback state.
         * We've already synced playback state from JS so the correct method should be chosen.
         */
        override fun onPlay() {
            evalJSWithTargetVideo { videoId -> "$videoId.play();" }
        }

        // See onPlay for details.
        override fun onPause() {
            evalJSWithTargetVideo { videoId -> "$videoId.pause();" }
        }

        override fun onSkipToNext() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_NEXT)
        override fun onSkipToPrevious() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        private fun dispatchKeyEventDownUp(keyCode: Int) {
            KEY_EVENT_ACTIONS_DOWN_UP.forEach { action -> activity.dispatchKeyEvent(KeyEvent(action, keyCode)) }
        }

        override fun onSeekTo(absolutePositionMillis: Long) {
            val absolutePositionSeconds = TimeUnit.MILLISECONDS.toSeconds(absolutePositionMillis)
            evalJSWithTargetVideo { videoId -> "$videoId.currentTime = $absolutePositionSeconds;" }
        }
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
