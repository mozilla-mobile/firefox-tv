/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.app.Activity
import android.arch.lifecycle.Lifecycle.Event.ON_DESTROY
import android.arch.lifecycle.Lifecycle.Event.ON_START
import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import android.arch.lifecycle.LifecycleObserver
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
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.webkit.JavascriptInterface
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.iwebview.IWebView
import java.util.concurrent.TimeUnit

private const val JS_INTERFACE_IDENTIFIER = "_firefoxTV_playbackStateObserverJava"
private const val MEDIA_SESSION_TAG = "FirefoxTVMedia"

private const val SUPPORTED_ACTIONS = ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or
        ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS or
        ACTION_SEEK_TO // "Alexa, rewind/fast-forward <num> <unit-of-time>"

// See `onSeekTo` for details on HACKED_*.
private const val HACKED_PLAYBACK_POSITION: Long = Long.MAX_VALUE / 2
private const val HACKED_PLAYBACK_SPEED: Float = 0.0f

private val KEY_EVENT_ACTIONS_DOWN_UP = listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)

/**
 * An encapsulation of a [MediaSessionCompat] instance to allow voice commands on videos; we
 * handle some hardware keys here too: see [MediaSessionCallbacks].
 *
 * Before use, callers should:
 * - Add this as a [LifecycleObserver]
 * - Add [dispatchKeyEvent] to KeyEvent handling
 *
 * To save time, we don't handle audio through either voice or the remote play/pause button: we don't
 * explicitly handle playback changes ourselves and we mute play/pause events from being received
 * by the page (see [dispatchKeyEvent]).
 *
 * To save time, this implementation doesn't conform to the [MediaSessionCompat] API but still
 * functions correctly. Conforming to the API requires knowing about playback state, which is
 * complicated because we need to sync Java state with DOM state. For the current feature set,
 * this appears to be unnecessary. "Alexa play/pause" will send either event based on the current
 * playback state so if we lock the initial playback state to PLAYING [1], we can respond to
 * `onPlay/Pause` events by playing the video if it's paused or pausing the video if it's playing.
 *
 * The constructor should be called from the UiThread because of [mediaSession].
 *
 * [1]: If the initial playback state is PAUSED or NONE, a music selection voice conversation
 * overrides our voice commands.
 */
class VideoVoiceCommandMediaSession @UiThread constructor(
        private val activity: Activity,
        private val getIWebView: () -> IWebView?
) : LifecycleObserver {

    @get:UiThread // mediaSession is not thread safe.
    private val mediaSession = MediaSessionCompat(activity, MEDIA_SESSION_TAG)
    private val cachedPlaybackState = PlaybackStateCompat.Builder()
            .setActions(SUPPORTED_ACTIONS)


    init {
        val playbackState = cachedPlaybackState
                // See class javadoc for details on STATE_PLAYING.
                // See `onSeekTo` for details on HACKED_*.
                .setState(STATE_PLAYING, HACKED_PLAYBACK_POSITION, HACKED_PLAYBACK_SPEED)
                .build()
        mediaSession.setPlaybackState(playbackState)
        mediaSession.setCallback(MediaSessionCallbacks())
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        mediaSession.isActive = true
    }

    @OnLifecycleEvent(ON_STOP)
    fun onStop() {
        // Video playback stops when backgrounded so we can end the session.
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

    inner class JavascriptVideoPlaybackStateSyncer {
        @JavascriptInterface
        fun setIsAnyVideoPlaying(isAnyVideoPlaying: Boolean) {
            launch(UI) { // mediaSession is on UI thread only.
                val playbackStateString = if (isAnyVideoPlaying) STATE_PLAYING else STATE_PAUSED
                val playbackState = cachedPlaybackState
                        .setState(playbackStateString, HACKED_PLAYBACK_POSITION, HACKED_PLAYBACK_SPEED)
                        .build()

                mediaSession.setPlaybackState(playbackState)
            }
        }
    }

    /**
     * Callbacks for voice commands ("Alexa play") and hardware media buttons on key down. See
     * [dispatchKeyEvent] for more details on hardware media button propagation.
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

        override fun onPlay() {
            onPlayPause() // See class javadoc for details.
        }

        override fun onPause() {
            onPlayPause() // See class javadoc for details.
        }

        private fun onPlayPause() {
            // Due to time constraints, this code is written for a single video on the page,
            // which should cover the majority use case.
            //
            // We don't handle audio: see class javadoc for details.
            getIWebView()?.evalJS("""
                |(function() {
                |    $GET_TARGET_VIDEO_OR_RETURN
                |
                |    if ($ID_TARGET_VIDEO.paused) {
                |        $ID_TARGET_VIDEO.play();
                |    } else {
                |       $ID_TARGET_VIDEO.pause();
                |   }
                |})();
                """.trimMargin())
        }

        override fun onSkipToNext() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_NEXT)
        override fun onSkipToPrevious() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        private fun dispatchKeyEventDownUp(keyCode: Int) {
            KEY_EVENT_ACTIONS_DOWN_UP.forEach { action -> activity.dispatchKeyEvent(KeyEvent(action, keyCode)) }
        }

        override fun onSeekTo(absolutePositionMillis: Long) {
            // This method is called for "fast-forward/rewind X <time-unit>" where the args are an
            // absolute position. The system calculates the absolute position from the current
            // playback position and the user-provided offset.
            //
            // The MediaSession API calculates the current playback time with the last "current time"
            // value and the playback speed we provide it (in MediaSession.setPlaybackState):
            //   current_time = time_passed * playback_speed + initial_time
            //
            // However, we force STATE_PLAYING for the duration the app is open so this calculated
            // playback time will be inaccurate to the actual video. We hack around this by setting
            // HACKED_PLAYBACK_SPEED to 0 and never updating the current playback time so the
            // calculated position never changes: we can use this to calculate the offset (see the code).
            //
            // The system will never provide a negative absolute position so if HACKED_PLAYBACK_POSITION
            // is 0, we cannot calculate negative offsets and thus can't rewind). To avoid this,
            // we set it to the middle-most value (MAX_VALUE / 2) to support the widest range of
            // playback.
            //
            // This will break if absolute seeking is implemented (#941).
            val offsetMillis = absolutePositionMillis - HACKED_PLAYBACK_POSITION
            val offsetSeconds = TimeUnit.MILLISECONDS.toSeconds(offsetMillis)
            getIWebView()?.evalJS("""
                |(function() {
                |    $GET_TARGET_VIDEO_OR_RETURN
                |
                |    $ID_TARGET_VIDEO.currentTime = $ID_TARGET_VIDEO.currentTime + $offsetSeconds
                |})();
                """.trimMargin())
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
 */
private val JS_OBSERVE_PLAYBACK_STATE = """
var _firefoxTV_isPlaybackStateObserverLoaded;
(function () {
    const PLAYBACK_STATE_CHANGE_EVENTS = ['play', 'pause'];

    const videosWithListeners = new Set();

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
        _firefoxTV_playbackStateObserverJava.setIsAnyVideoPlaying(isAnyVideoPlaying());
    }

    function isAnyVideoPlaying() {
        return Array.from(document.querySelectorAll('video')).some(video => !video.paused);
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
