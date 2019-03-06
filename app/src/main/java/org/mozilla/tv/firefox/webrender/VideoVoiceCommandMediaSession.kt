/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.ACTION_PAUSE
import android.media.session.PlaybackState.ACTION_PLAY
import android.media.session.PlaybackState.ACTION_PLAY_PAUSE
import android.media.session.PlaybackState.ACTION_SEEK_TO
import android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT
import android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS
import android.media.session.PlaybackState.PLAYBACK_POSITION_UNKNOWN
import android.media.session.PlaybackState.STATE_BUFFERING
import android.media.session.PlaybackState.STATE_PAUSED
import android.media.session.PlaybackState.STATE_PLAYING
import android.media.session.PlaybackState.STATE_STOPPED
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.webrender.VideoVoiceCommandMediaSession.MediaSessionCallbacks
import org.mozilla.tv.firefox.ext.addJavascriptInterface
import org.mozilla.tv.firefox.ext.observePlaybackState
import org.mozilla.tv.firefox.ext.pauseAllVideoPlaybacks
import org.mozilla.tv.firefox.ext.pauseTargetVideo
import org.mozilla.tv.firefox.ext.playTargetVideo
import org.mozilla.tv.firefox.ext.removeJavascriptInterface
import org.mozilla.tv.firefox.ext.seekTargetVideoToPosition
import org.mozilla.tv.firefox.telemetry.MediaSessionEventType
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

private const val JS_INTERFACE_IDENTIFIER = "_firefoxTV_playbackStateObserverJava"
private const val MEDIA_SESSION_TAG = "FirefoxTVMedia"

private const val SUPPORTED_ACTIONS = ACTION_PLAY_PAUSE or ACTION_PLAY or ACTION_PAUSE or
        ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS or
        ACTION_SEEK_TO // "Alexa, rewind/fast-forward <num> <unit-of-time>"

private val KEY_EVENT_ACTIONS_DOWN_UP = listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)
private val KEY_CODES_MEDIA_NEXT_PREV = listOf(KEYCODE_MEDIA_NEXT, KEYCODE_MEDIA_PREVIOUS)
private val KEY_CODES_MEDIA_PLAY_PAUSE = listOf(KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE, KEYCODE_MEDIA_PLAY_PAUSE)

/**
 * An encapsulation of a [MediaSession] instance to allow voice commands on videos; we
 * handle some hardware keys here too: see [MediaSessionCallbacks].
 *
 * Before use, callers should:
 * - Add this as a [LifecycleObserver]
 * - Add [dispatchKeyEvent] to KeyEvent handling
 * - Call [onCreateEngineView] and [onDestroyEngineView] for fragment lifecycle handling.
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

    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread // MediaSession is not thread safe.
    private val mediaSession = MediaSession(activity, MEDIA_SESSION_TAG)

    /* Since we may update playback state often, we cache this builder to reduce allocation. */
    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread // PlaybackStateCompat.Builder is not thread safe.
    private val cachedPlaybackStateBuilder = PlaybackState.Builder()
            .setActions(SUPPORTED_ACTIONS)

    private var engineView: EngineView? = null
    private var sessionIsLoadingObserver: SessionIsLoadingObserver? = null

    private var isLifecycleResumed = false
    private var isLifecycleStarted = false

    private val uiLifecycleCancelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiLifecycleCancelJob)

    init {
        mediaSession.setCallback(MediaSessionCallbacks())
        @Suppress("DEPRECATION")
        // According to Android, these should now be handled by all MediaSession
        // implementations. However it doesn't seem that Fire OS has caught up
        // yet, so we still need these
        //
        // To test this, verify that the remote play/pause button works on YouTube
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
    }

    fun onCreateEngineView(engineView: EngineView, session: Session) {
        this.engineView = engineView.apply {
            addJavascriptInterface(JavascriptVideoPlaybackStateSyncer(), JS_INTERFACE_IDENTIFIER)
        }

        val sessionIsLoadingObserver = SessionIsLoadingObserver(engineView, session)
        session.register(sessionIsLoadingObserver, owner = activity)
        this.sessionIsLoadingObserver = sessionIsLoadingObserver
    }

    fun onDestroyEngineView(engineView: EngineView, session: Session) {
        engineView.removeJavascriptInterface(JS_INTERFACE_IDENTIFIER)
        this.engineView = null

        session.unregister(sessionIsLoadingObserver!!)
        this.sessionIsLoadingObserver = null
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume() {
        isLifecycleResumed = true
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun onPause() {
        isLifecycleResumed = false
    }

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        isLifecycleStarted = true

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
        isLifecycleStarted = false

        // Videos playing when the app was backgrounded get into an inconsistent state: `paused`
        // will return false but they won't actually be playing. This makes any user "play" commands
        // take two clicks: one to "pause" and the second to play for real. We get around this by
        // pausing all videos before being backgrounded. This issue doesn't affect pages with
        // autoplay, like YouTube.
        //
        // The videos may send playback state update events to Java, which we're forced to ignore:
        // see JavascriptVideoPlaybackStateSyncer for the code.
        engineView?.pauseAllVideoPlaybacks()

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
        uiLifecycleCancelJob.cancel()
    }

    /**
     * Potentially handles key events before they are received by the system for standard handling
     * (e.g. dispatch to the View hierarchy). If the keys are unhandled here, the [mediaSession]
     * does key handling (after this) as part of standard handling: see
     * [MediaSessionCallbacks.onMediaButtonEvent].
     *
     * @return true if the key event is handled and no one else should handle it, false otherwise.
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean = when (event.keyCode) {
        // Prevent the WebView (and unfortunately anyone else) from handling media play/pause up events:
        // our MediaSession handles the ACTION_DOWN event and will inject our JS to play/pause.
        // However, MediaSession ignores the ACTION_UP event, which instead will be handled by the
        // page. The page can then redundantly handle the play/pause event, undoing the MediaSession
        // action. To prevent this, we swallow play/pause key up events and handle it all via MediaSession.
        KEYCODE_MEDIA_PLAY_PAUSE, KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE -> event.action == KeyEvent.ACTION_UP
        else -> false
    }

    class SessionIsLoadingObserver(private val engineView: EngineView, private val session: Session) : Session.Observer {
        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            if (!loading) {
                engineView.observePlaybackState() // Calls through to JavascriptVideoPlaybackStateSyncer.
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
        fun syncPlaybackState(
            isVideoPresent: Boolean,
            isVideoPlaying: Boolean,
            jsPositionSeconds: Double,
            jsPlaybackSpeed: Float
        ) {
            // During onStop we pause all videos which may send playback state updates to this
            // method. In theory, since the JS is async, this could undo the playback state we set
            // in onStop so we ignore these updates. In practice, this method doesn't appear to be
            // called from that JS but we leave this in for safety.
            if (!isLifecycleStarted) { return }

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

            uiScope.launch { // mediaSession and cachedPlaybackState is on UI thread only.
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
    inner class MediaSessionCallbacks : MediaSession.Callback() {
        /**
         * Potentially handles key events before they are received by the [mediaSession]: this is
         * called while the system handles key events. This is *only called for key down events*:
         * key up events continue through standard handling.
         *
         * Note: [dispatchKeyEvent] has an opportunity to handle keys before this method.
         *
         * @return true for MediaSession to not handle the event but to continue system handling,
         * false for MediaSession to handle the event and stop system handling.
         */
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val key = mediaButtonEvent.getParcelableExtra<KeyEvent?>(Intent.EXTRA_KEY_EVENT)

            if (KEY_CODES_MEDIA_PLAY_PAUSE.contains(key?.keyCode)) {
                // Our overall goal is to see how often voice commands are used. play/pause are the
                // only keys on a standard Alexa remote that also have voice commands so it's the
                // only one we need to record in order to disambiguate voice commands from buttons.
                TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.PLAY_PAUSE_BUTTON)
            }

            // Forward media next/prev events to the WebView: the WebView already receives key up
            // events and we prevent MediaSession from handling the down events by not calling super.
            // If MediaSession handled the key down events, it'd call onSkipToNext which dispatches
            // additional key events and we'd have an infinite loop.
            if (key != null &&
                    key.action == KeyEvent.ACTION_DOWN && KEY_CODES_MEDIA_NEXT_PREV.contains(key.keyCode)) {
                return true
            }

            return super.onMediaButtonEvent(mediaButtonEvent)
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
            engineView?.playTargetVideo()
            TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.PLAY)
        }

        // See onPlay for details.
        override fun onPause() {
            // If we receive a MediaSession callback while the app is paused, it's coming from a
            // voice command (which pauses the app to handle them).
            engineView?.pauseTargetVideo(!isLifecycleResumed)
            TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.PAUSE)
        }

        override fun onSkipToNext() {
            dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_NEXT)
            TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.NEXT)
        }

        override fun onSkipToPrevious() {
            dispatchKeyEventDownUp(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.PREV)
        }

        private fun dispatchKeyEventDownUp(keyCode: Int) {
            KEY_EVENT_ACTIONS_DOWN_UP.forEach { action -> activity.dispatchKeyEvent(KeyEvent(action, keyCode)) }
        }

        override fun onSeekTo(absolutePositionMillis: Long) {
            val absolutePositionSeconds = TimeUnit.MILLISECONDS.toSeconds(absolutePositionMillis)
            engineView?.seekTargetVideoToPosition(absolutePositionSeconds)
            TelemetryIntegration.INSTANCE.mediaSessionEvent(MediaSessionEventType.SEEK)
        }
    }
}
