/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.app.Activity
import android.arch.lifecycle.Lifecycle.Event.ON_DESTROY
import android.arch.lifecycle.Lifecycle.Event.ON_START
import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import org.mozilla.focus.iwebview.IWebView

private const val MEDIA_SESSION_TAG = "FirefoxTVMedia"

private const val SUPPORTED_ACTIONS = ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or
        ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS

private val KEY_EVENT_ACTIONS_DOWN_UP = listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)

/**
 * An encapsulation of a [MediaSessionCompat] instance to allow voice commands on videos; we
 * handle some hardware keys here too: see [MediaSessionCallbacks].
 *
 * Before use, callers should:
 * - Add this as a [LifecycleObserver]
 * - Assign [iWebView]
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
 * [1]: If the initial playback state is PAUSED or NONE, a music selection voice conversation
 * overrides our voice commands.
 */
class VideoVoiceCommandMediaSession(private val activity: Activity) : LifecycleObserver {

    private val mediaSession = MediaSessionCompat(activity, MEDIA_SESSION_TAG)

    var iWebView: IWebView? = null // Expected to be set by caller.

    init {
        val pb = PlaybackStateCompat.Builder()
                .setActions(SUPPORTED_ACTIONS)

                // See class javadoc for details on STATE_PLAYING.
                .setState(STATE_PLAYING, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        mediaSession.setPlaybackState(pb)
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
    fun onDestroy(lifecycleOwner: LifecycleOwner) {
        // Fragments can be re-used after onDestroy so we must remove the observer
        // so we don't have two instances when this object is created again.
        lifecycleOwner.lifecycle.removeObserver(this)

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

    /**
     * Callbacks for voice commands ("Alexa play") and hardware media buttons on key down. See
     * [dispatchKeyEvent] for more details on hardware media button propagation.
     */
    inner class MediaSessionCallbacks : MediaSessionCompat.Callback() {
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
            iWebView?.evalJS("""
                |(function() {
                |    var videos = Array.from(document.querySelectorAll('video'));
                |    if (videos.length === 0) { return; }
                |
                |    var videoToOperateOn = videos.find(function (video) { return !video.paused });
                |    if (!videoToOperateOn) {
                |        videoToOperateOn = videos[0];
                |    }
                |
                |    if (videoToOperateOn.paused) {
                |        videoToOperateOn.play();
                |    } else {
                |       videoToOperateOn.pause();
                |   }
                |})();
                """.trimMargin())
        }

        override fun onSkipToNext() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_NEXT)
        override fun onSkipToPrevious() = dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        private fun dispatchKeyEventDownUp(keyCode: Int) {
            KEY_EVENT_ACTIONS_DOWN_UP.forEach { action -> activity.dispatchKeyEvent(KeyEvent(action, keyCode)) }
        }
    }
}
