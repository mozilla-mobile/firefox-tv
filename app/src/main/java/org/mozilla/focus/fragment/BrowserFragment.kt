/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.UiThread
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.browser.cursor.CursorController
import org.mozilla.focus.session.NullSession
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionCallbackProxy
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.web.IWebViewLifecycleFragment
import org.mozilla.focus.widget.BrowserNavigationOverlay
import org.mozilla.focus.widget.InlineAutocompleteEditText
import org.mozilla.focus.widget.NavigationEvent

private const val ARGUMENT_SESSION_UUID = "sessionUUID"

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : IWebViewLifecycleFragment(),
        BrowserNavigationOverlay.BrowserNavigationStateProvider {
    companion object {
        const val FRAGMENT_TAG = "browser"

        @JvmStatic
        fun createForSession(session: Session) = BrowserFragment().apply {
            arguments = Bundle().apply { putString(ARGUMENT_SESSION_UUID, session.uuid) }
        }
    }

    // We need to respond to the onPageFinished event so we set a flag here.
    var isReloadingForYoutubeDrawerClosed = false

    // IWebViewLifecycleFragment expects a value for these properties before onViewCreated. We use a getter
    // for the properties that reference session because it is lateinit.
    override lateinit var session: Session
    override val initialUrl get() = session.url.value
    override val iWebViewCallback get() = SessionCallbackProxy(session, BrowserIWebViewCallback(this))

    // getUrl() is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    //
    // Note: when refactoring, I removed the url view and replaced urlView.setText with assignment
    // to this url variable - should be equivalent.
    var url: String? = null
        private set

    private val sessionManager = SessionManager.getInstance()

    /**
     * Encapsulates the cursor's components. If this value is null, the Cursor is not attached
     * to the view hierarchy.
     */
    var cursor: CursorController? = null
        @UiThread get set // Set from the UI thread so serial access is required for simplicity.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.browserOverlay!!.onNavigationEvent = onNavigationEvent
        view.browserOverlay!!.navigationStateProvider = this
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initSession() {
        val sessionUUID = arguments.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")
        session = if (sessionManager.hasSessionWithUUID(sessionUUID))
            sessionManager.getSessionByUUID(sessionUUID)
        else
            NullSession()

        setBlockingEnabled(session.isBlockingEnabled)
        session.url.observe(this, Observer { url -> this@BrowserFragment.url = url })
        session.loading.observe(this, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(loading: Boolean) {
                if (!loading && isReloadingForYoutubeDrawerClosed) {
                    isReloadingForYoutubeDrawerClosed = false

                    // We send a play event which:
                    // - If we're on the video selection page, does nothing.
                    // - If we're in a fullscreen video, will show the play/pause controls on the screen so
                    // we don't just see a black screen.
                    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                }

                // Update state on load start and finish to ensure buttons are updated correctly
                if (browserOverlay.isVisible) {
                    browserOverlay.updateNavigationButtons()
                }
            }
        })
    }

    private val onNavigationEvent = { event: NavigationEvent, value: String?,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? ->
        when (event) {
            NavigationEvent.BACK -> if (canGoBack()) goBack()
            NavigationEvent.FORWARD -> if (canGoForward()) goForward()
            NavigationEvent.RELOAD -> reload()
            NavigationEvent.HOME -> ScreenController.showHomeScreen(fragmentManager, activity as OnUrlEnteredListener)
            NavigationEvent.SETTINGS -> ScreenController.showSettingsScreen(fragmentManager)
            NavigationEvent.LOAD -> {
                (activity as MainActivity).onTextInputUrlEntered(value!!, autocompleteResult!!, UrlTextInputLocation.MENU)
                showOverlay(false)
            }
            NavigationEvent.RELOAD_YT -> {
                isReloadingForYoutubeDrawerClosed = true
                reload()
            }
        }
        Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)
        cursor = CursorController(this, cursorParent = layout.browserFragmentRoot,
                view = layout.cursorView)
        lifecycle.addObserver(cursor!!)
        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycle.removeObserver(cursor!!)
        cursor = null
    }

    override fun isBackEnabled() = canGoBack()
    override fun isForwardEnabled() = canGoForward()
    override fun getCurrentUrl() = url

    fun onBackPressed(): Boolean {
        when {
            canGoBack() -> {
                goBack()
                TelemetryWrapper.browserBackControllerEvent()
            }
            browserOverlay.isVisible -> browserOverlay.setOverlayVisibleByUser(false)
            else -> {
                fragmentManager.popBackStack()
                SessionManager.getInstance().removeCurrentSession()
            }
        }
        return true
    }

    // TODO: Remove these eventually.
    // This is outside BrowserFragment's responsibilities - these should be called through a WebView interface.
    fun canGoForward() = webview?.canGoForward() ?: false
    fun canGoBack() = webview?.canGoBack() ?: false
    fun goBack() = webview?.goBack()
    fun goForward() = webview?.goForward()

    fun loadUrl(url: String) {
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url)
        }
    }

    fun reload() = webView?.reload()
    fun setBlockingEnabled(enabled: Boolean) = webview?.setBlockingEnabled(enabled)

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        /**
         * Key handling order:
         * - Menu to control overlay
         * - Youtube remap of BACK to ESC
         * - Cursor
         * - Return false, as unhandled
         */
        return (handleSpecialKeyEvent(event)) ||
                (cursor?.keyDispatcher?.dispatchKeyEvent(event) ?: false)
    }

    private fun handleSpecialKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            val toShow = !browserOverlay.isVisible
            showOverlay(toShow)
            // Fix this youtube focus hack in #393
            if (!toShow && webView!!.isYoutubeTV) {
                webView?.requestFocus()
            }
            return true
        }

        if (!browserOverlay.isVisible && webView!!.isYoutubeTV &&
                event.keyCode == KeyEvent.KEYCODE_BACK) {
            val escKeyEvent = KeyEvent(event.action, KeyEvent.KEYCODE_ESCAPE)
            activity.dispatchKeyEvent(escKeyEvent)
            return true
        }
        return false
    }

    private fun showOverlay(toShow: Boolean) {
        browserOverlay.setOverlayVisibleByUser(toShow)
        if (toShow) cursor?.onPause() else cursor?.onResume()
        cursor?.setEnabledForCurrentState()
    }
}

private class BrowserIWebViewCallback(
        private val browserFragment: BrowserFragment
) : IWebView.Callback {

    private var fullscreenCallback: IWebView.FullscreenCallback? = null

    override fun onPageStarted(url: String) {}

    override fun onPageFinished(isSecure: Boolean) {}
    override fun onProgress(progress: Int) {}

    override fun onURLChanged(url: String) {}
    override fun onRequest(isTriggeredByUserGesture: Boolean) {}

    override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {}

    override fun onLongPress(hitTarget: IWebView.HitTarget) {}

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
        fullscreenCallback = callback
        if (view == null) return

        with (browserFragment) {
            // Hide browser UI and web content
            browserContainer.visibility = View.INVISIBLE

            val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            videoContainer.addView(view, params)
            videoContainer.visibility = View.VISIBLE
        }
    }

    override fun onExitFullScreen() {
        with (browserFragment) {
            videoContainer.removeAllViews()
            videoContainer.visibility = View.GONE

            browserContainer.visibility = View.VISIBLE
        }

        fullscreenCallback?.fullScreenExited()
        fullscreenCallback = null
    }
}
