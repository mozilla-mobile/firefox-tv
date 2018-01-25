/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.arch.lifecycle.Observer
import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.ext.isVoiceViewEnabled
import org.mozilla.focus.session.NullSession
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionCallbackProxy
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Direction
import org.mozilla.focus.utils.Edge
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.widget.Cursor
import org.mozilla.focus.widget.CursorEvent

private const val ARGUMENT_SESSION_UUID = "sessionUUID"
private const val SCROLL_MULTIPLIER = 45

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : WebFragment(), CursorEvent {
    companion object {
        const val FRAGMENT_TAG = "browser"

        @JvmStatic
        fun createForSession(session: Session): BrowserFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_SESSION_UUID, session.uuid)

            val fragment = BrowserFragment()
            fragment.arguments = arguments

            return fragment
        }
    }

    private var cursor: Cursor? = null

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private var videoContainer: ViewGroup? = null

    /**
     * Container containing the browser chrome and web content.
     */
    private var browserContainer: View? = null

    private var fullscreenCallback: IWebView.FullscreenCallback? = null

    // getUrl() is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    //
    // Note: when refactoring, I removed the url view and replaced urlView.setText with assignment
    // to this url variable - should be equivalent.
    var url: String? = null
        private set
    private val sessionManager: SessionManager
    private var session: Session? = null

    val cursorLocation: Point
        get() = cursor!!.location

    private val scrollVelocity: Int
        get() {
            val speed = cursor!!.speed.toInt()
            return speed * SCROLL_MULTIPLIER
        }

    init {
        sessionManager = SessionManager.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionUUID = arguments.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")

        session = if (sessionManager.hasSessionWithUUID(sessionUUID))
            sessionManager.getSessionByUUID(sessionUUID)
        else
            NullSession()
    }

    override fun onResume() {
        super.onResume()
        val activity = activity
        (activity as? MainActivity)?.updateHintNavigationVisibility(MainActivity.VideoPlayerState.BROWSER)
    }

    override fun getSession(): Session? {
        return session
    }

    override fun getInitialUrl(): String? {
        return session!!.url.value
    }

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        cursor = view.findViewById<View>(R.id.cursor) as Cursor
        cursor!!.cursorEvent = this

        videoContainer = view.findViewById<View>(R.id.video_container) as ViewGroup
        browserContainer = view.findViewById(R.id.browser_container)

        session!!.url.observe(this, Observer { url -> this@BrowserFragment.url = url })

        setBlockingEnabled(session!!.isBlockingEnabled)

        session!!.loading.observe(this, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(loading: Boolean) {
                val activity = activity as MainActivity
                updateCursorState()
                if (!loading && activity.isReloadingForYoutubeDrawerClosed) {
                    activity.isReloadingForYoutubeDrawerClosed = false

                    // We send a play event which:
                    // - If we're on the video selection page, does nothing.
                    // - If we're in a fullscreen video, will show the play/pause controls on the screen so
                    // we don't just see a black screen.
                    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                }
            }
        })

        return view
    }

    override fun createCallback(): IWebView.Callback {
        return SessionCallbackProxy(session, object : IWebView.Callback {
            override fun onPageStarted(url: String) {}

            override fun onPageFinished(isSecure: Boolean) {}

            override fun onURLChanged(url: String) {}

            override fun onRequest(isTriggeredByUserGesture: Boolean) {}

            override fun onProgress(progress: Int) {}

            override fun countBlockedTracker() {}

            override fun resetBlockedTrackers() {}

            override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {}

            override fun onLongPress(hitTarget: IWebView.HitTarget) {}

            override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
                fullscreenCallback = callback

                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer!!.visibility = View.INVISIBLE

                    // Add view to video container and make it visible
                    val params = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    videoContainer!!.addView(view, params)
                    videoContainer!!.visibility = View.VISIBLE

                    // Switch to immersive mode: Hide system bars other UI controls
                    switchToImmersiveMode()
                }
            }

            override fun onExitFullScreen() {
                // Remove custom video views and hide container
                videoContainer!!.removeAllViews()
                videoContainer!!.visibility = View.GONE

                // Show browser UI and web content again
                browserContainer!!.visibility = View.VISIBLE

                exitImmersiveModeIfNeeded()

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback!!.fullScreenExited()
                    fullscreenCallback = null
                }
            }
        })
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private fun switchToImmersiveMode() {
        val activity = activity ?: return

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * Show the system bars again.
     */
    private fun exitImmersiveModeIfNeeded() {
        val activity = activity ?: return

        if (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON and activity.window.attributes.flags == 0) {
            // We left immersive mode already.
            return
        }

        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onDestroy() {
        super.onDestroy()

        // This fragment might get destroyed before the user left immersive mode (e.g. by opening another URL from an app).
        // In this case let's leave immersive mode now when the fragment gets destroyed.
        exitImmersiveModeIfNeeded()
    }

    fun onBackPressed(): Boolean {
        if (canGoBack()) {
            // Go back in web history
            goBack()
            TelemetryWrapper.browserBackControllerEvent()
        } else {
            fragmentManager.popBackStack()
            SessionManager.getInstance().removeCurrentSession()
        }

        return true
    }

    fun canGoForward(): Boolean {
        val webView = webView
        return webView != null && webView.canGoForward()
    }

    fun canGoBack(): Boolean {
        val webView = webView
        return webView != null && webView.canGoBack()
    }

    fun goBack() {
        val webView = webView
        webView?.goBack()
    }

    fun goForward() {
        val webView = webView
        webView?.goForward()
    }

    fun loadUrl(url: String) {
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url)
        }
    }

    fun reload() {
        val webView = webView
        webView?.reload()
    }

    fun setBlockingEnabled(enabled: Boolean) {
        val webView = webView
        webView?.setBlockingEnabled(enabled)
    }

    // --- TODO: CURSOR CODE - MODULARIZE IN #412. --- //
    /**
     * Gets the current state of the application and updates the cursor state accordingly.
     *
     * Note that this pattern could use some improvements:
     * - It's a little weird to get the current state from globals, rather than get passed in relevant values.
     * - BrowserFragment.setCursorEnabled should be called from this code path, but that's unclear
     * - BrowserFragment should use a listener to talk to MainActivity and shouldn't know about it directly.
     * - BrowserFragment calls MainActivity which calls BrowserFragment again - this is unnecessary.
     */
    fun updateCursorState() {
        val activity = activity as MainActivity
        val webView = webView
        // Bandaid null checks, underlying issue #249
        val enableCursor = webView != null &&
                webView.getUrl() != null &&
                !webView.getUrl()!!.contains("youtube.com/tv") &&
                context != null &&
                !context.isVoiceViewEnabled() // VoiceView has its own navigation controls.
        activity.setCursorEnabled(enableCursor)
    }

    fun moveCursor(direction: Direction) {
        cursor!!.moveCursor(direction)
    }

    fun stopMoving(direction: Direction) {
        cursor!!.stopMoving(direction)
    }

    fun setCursorEnabled(toEnable: Boolean) {
        cursor!!.visibility = if (toEnable) View.VISIBLE else View.GONE
    }

    override fun cursorHitEdge(edge: Edge) {
        val webView = webView ?: return

        when (edge) {
            Edge.TOP -> webView.flingScroll(0, -scrollVelocity)
            Edge.BOTTOM -> webView.flingScroll(0, scrollVelocity)
            Edge.LEFT -> webView.flingScroll(-scrollVelocity, 0)
            Edge.RIGHT -> webView.flingScroll(scrollVelocity, 0)
        }
    }
}
