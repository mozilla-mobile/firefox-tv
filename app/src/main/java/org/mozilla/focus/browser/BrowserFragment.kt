/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

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
import org.mozilla.focus.MainActivity
import org.mozilla.focus.R
import org.mozilla.focus.ScreenController
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.browser.cursor.CursorController
import org.mozilla.focus.ext.isVisible
import org.mozilla.focus.ext.toUri
import org.mozilla.focus.home.BundledTilesManager
import org.mozilla.focus.home.CustomTilesManager
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.iwebview.IWebViewLifecycleFragment
import org.mozilla.focus.session.NullSession
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionCallbackProxy
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.ViewUtils.showCenteredTopToast
import org.mozilla.focus.widget.InlineAutocompleteEditText

private const val ARGUMENT_SESSION_UUID = "sessionUUID"

private const val TOAST_Y_OFFSET = 200

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : IWebViewLifecycleFragment() {
    companion object {
        const val FRAGMENT_TAG = "browser"
        const val URL_HOME = "firefox:home"

        @JvmStatic
        fun createForSession(session: Session) = BrowserFragment().apply {
            arguments = Bundle().apply { putString(ARGUMENT_SESSION_UUID, session.uuid) }
        }
    }

    // IWebViewLifecycleFragment expects a value for these properties before onViewCreated. We use a getter
    // for the properties that reference session because it is lateinit.
    override lateinit var session: Session
    override val initialUrl get() = session.url.value
    override val iWebViewCallback get() = SessionCallbackProxy(session, BrowserIWebViewCallback(this))

    /**
     * The current URL.
     *
     * Use this instead of the WebView's URL which can return null, return a null URL, or return
     * data: URLs (for error pages).
     */
    var url: String? = null
        private set

    private val sessionManager = SessionManager.getInstance()

    /**
     * Encapsulates the cursor's components. If this value is null, the Cursor is not attached
     * to the view hierarchy.
     */
    var cursor: CursorController? = null
        @UiThread get set // Set from the UI thread so serial access is required for simplicity.

    // Cache the overlay visibility state to persist in fragment back stack
    private var overlayVisibleCached: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
    }

    private fun initSession() {
        val sessionUUID = arguments.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")
        session = if (sessionManager.hasSessionWithUUID(sessionUUID))
            sessionManager.getSessionByUUID(sessionUUID)
        else
            NullSession()

        webView?.setBlockingEnabled(session.isBlockingEnabled)
        session.url.observe(this, Observer { url -> this@BrowserFragment.url = url })
        session.loading.observe(this, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(loading: Boolean) {
                // Update state on load start and finish to ensure buttons are updated correctly
                if (browserOverlay.isVisible) {
                    browserOverlay.updateOverlayForCurrentState()
                }
            }
        })
    }

    private val onNavigationEvent = { event: NavigationEvent, value: String?,
            autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? ->
        when (event) {
            NavigationEvent.BACK -> if (webView?.canGoBack() ?: false) webView?.goBack()
            NavigationEvent.FORWARD -> if (webView?.canGoForward() ?: false) webView?.goForward()
            NavigationEvent.TURBO, NavigationEvent.RELOAD -> webView?.reload()
            NavigationEvent.HOME -> ScreenController.showHomeScreen(fragmentManager, activity as OnUrlEnteredListener)
            NavigationEvent.SETTINGS -> ScreenController.showSettingsScreen(fragmentManager)
            NavigationEvent.LOAD_URL -> {
                (activity as MainActivity).onTextInputUrlEntered(value!!, autocompleteResult!!, UrlTextInputLocation.MENU)
                setOverlayVisibileByUser(false)
            }
            NavigationEvent.LOAD_TILE -> {
                (activity as MainActivity).onNonTextInputUrlEntered(value!!)
                setOverlayVisibileByUser(false)
            }
            NavigationEvent.PIN_ACTION -> {
                this@BrowserFragment.url?.let { url ->
                    when (value) {
                        NavigationEvent.VAL_CHECKED -> {
                            CustomTilesManager.getInstance(context).pinSite(context, url,
                                    webView?.takeScreenshot())
                            browserOverlay.refreshTilesForInsertion()
                            showCenteredTopToast(context, R.string.notification_pinned_site, 0, TOAST_Y_OFFSET)
                        }
                        NavigationEvent.VAL_UNCHECKED -> {
                            url.toUri()?.let {
                                val tileId = BundledTilesManager.getInstance(context).unpinSite(context, it) ?: CustomTilesManager.getInstance(context).unpinSite(context, url)
                                // tileId should never be null, unless, for some reason we don't
                                // have a reference to the tile/the tile isn't a Bundled or Custom tile
                                if (tileId != null && !tileId.isEmpty()) {
                                    browserOverlay.removePinnedSiteFromTiles(tileId)
                                    showCenteredTopToast(context, R.string.notification_unpinned_site, 0, TOAST_Y_OFFSET)
                                }
                            }
                        }
                        else -> throw IllegalArgumentException("Unexpected value for PIN_ACTION: " + value)
                    }
                }
            }
        }
        Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        cursor = CursorController(this, cursorParent = layout.browserFragmentRoot,
                view = layout.cursorView)
        lifecycle.addObserver(cursor!!)

        with (layout.browserOverlay) {
            onNavigationEvent = this@BrowserFragment.onNavigationEvent
            navigationStateProvider = NavigationStateProvider()
            visibility = overlayVisibleCached ?: View.GONE
            onPreSetVisibilityListener = { isVisible ->
                // The overlay can clear the DOM and a previous focused element cache (e.g. reload)
                // so we need to do our own caching: see FocusedDOMElementCache for details.
                if (!isVisible) { webView?.focusedDOMElement?.cache() }
            }
        }

        layout.progressBar.initialize(this)

        return layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycle.removeObserver(cursor!!)
        cursor = null
        overlayVisibleCached = browserOverlay.visibility
    }

    fun onBackPressed(): Boolean {
        when {
            webView?.canGoBack() ?: false -> {
                webView?.goBack()
                TelemetryWrapper.browserBackControllerEvent()
            }
            browserOverlay.isVisible -> setOverlayVisibileByUser(false)
            else -> {
                fragmentManager.popBackStack()
                SessionManager.getInstance().removeCurrentSession()
            }
        }
        return true
    }

    fun loadUrl(url: String) {
        val webView = webView
        if (webView != null && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url)
        }
    }

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
        val keyCodeIsMenu = event.keyCode == KeyEvent.KEYCODE_MENU
        val keyCodeIsBack = event.keyCode == KeyEvent.KEYCODE_BACK
        val actionIsDown = event.action == KeyEvent.ACTION_DOWN
        val isOverlayToggleKey = (keyCodeIsMenu || (keyCodeIsBack && browserOverlay.isVisible))

        if (isOverlayToggleKey) {
            if (actionIsDown) {
                val toShow = !browserOverlay.isVisible
                setOverlayVisibileByUser(toShow)
                // Fix this youtube focus hack in #393
                if (!toShow && webView!!.isYoutubeTV) {
                    webView?.requestFocus()
                }
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

    /**
     * Changes the overlay visibility: this should be called instead of changing
     * [BrowserNavigationOverlay.isVisible] directly.
     *
     * It's important this is only called for user actions because our Telemetry
     * is dependent on it.
     */
    private fun setOverlayVisibileByUser(toShow: Boolean) {
        browserOverlay.visibility = if (toShow) View.VISIBLE else View.GONE
        if (toShow) cursor?.onPause() else cursor?.onResume()
        cursor?.setEnabledForCurrentState()
        TelemetryWrapper.drawerShowHideEvent(toShow)
    }

    private inner class NavigationStateProvider : BrowserNavigationOverlay.BrowserNavigationStateProvider {
        override fun isBackEnabled() = webView?.canGoBack() ?: false
        override fun isForwardEnabled() = webView?.canGoForward() ?: false
        override fun getCurrentUrl() = url
        override fun isURLPinned() = url.toUri()?.let {
            // TODO: #569 fix CustomTilesManager to use Uri too
            CustomTilesManager.getInstance(context).isURLPinned(it.toString()) ||
                    BundledTilesManager.getInstance(context).isURLPinned(it) } ?: false
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
