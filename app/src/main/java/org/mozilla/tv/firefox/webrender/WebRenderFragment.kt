/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.app.Activity
import android.os.Bundle
import android.support.annotation.UiThread
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebView
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.feature.session.SessionFeature
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.MediaSessionHolder
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.evalJS
import org.mozilla.tv.firefox.webrender.cursor.CursorController
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.ext.requireWebRenderComponents
import org.mozilla.tv.firefox.ext.isYoutubeTV
import org.mozilla.tv.firefox.ext.toList
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

private const val ARGUMENT_SESSION_UUID = "sessionUUID"

/**
 * Fragment for displaying the browser UI.
 */
class WebRenderFragment : EngineViewLifecycleFragment(), Session.Observer {
    companion object {
        const val FRAGMENT_TAG = "browser"

        @JvmStatic
        fun createForSession(session: Session) = WebRenderFragment().apply {
            arguments = Bundle().apply { putString(ARGUMENT_SESSION_UUID, session.id) }
        }
    }

    lateinit var session: Session

    private val mediaSessionHolder get() = activity as MediaSessionHolder? // null when not attached.

    /**
     * Encapsulates the cursor's components. If this value is null, the Cursor is not attached
     * to the view hierarchy.
     */
    var cursor: CursorController? = null
        @UiThread get set // Set from the UI thread so serial access is required for simplicity.

//    // Cache the overlay visibility state to persist in fragment back stack
//    private var overlayVisibleCached: Int? = null

    var sessionFeature: SessionFeature? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
    }

    private fun initSession() {
        val sessionUUID = arguments?.getString(ARGUMENT_SESSION_UUID)
                ?: throw IllegalAccessError("No session exists")
        session = context!!.webRenderComponents.sessionManager.findSessionById(sessionUUID) ?: NullSession.create()
        session.register(observer = this, owner = this)
    }

    override fun onFullScreenChanged(session: Session, enabled: Boolean) {
        val window = (context as? Activity)?.window ?: return
        val dontSleep = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (enabled) window.addFlags(dontSleep)
        else window.clearFlags(dontSleep)
    }

    override fun onDesktopModeChanged(session: Session, enabled: Boolean) {
        requireWebRenderComponents.sessionUseCases.requestDesktopSite.invoke(enabled, session)
    }

    override fun onContentPermissionRequested(session: Session, permissionRequest: PermissionRequest): Boolean =
        permissionRequest.grantIf { it is Permission.ContentProtectedMediaId }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        cursor = CursorController(this, cursorParent = layout.browserFragmentRoot,
                view = layout.cursorView)
        lifecycle.addObserver(cursor!!)

        layout.progressBar.initialize(this)

        // We break encapsulation here: we should use the super.webView reference but it's not init until
        // onViewCreated. However, overriding both onCreateView and onViewCreated in a single class
        // is confusing so I'd rather break encapsulation than confuse devs.
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onCreateWebView(layout.webview, session)

        return layout
    }

    override fun onWebViewCreated(webView: EngineView) {
        // The SessionFeature implementation will take care of making sure that we always render the currently selected
        // session in our engine view.
        sessionFeature = SessionFeature(
            requireWebRenderComponents.sessionManager,
            requireWebRenderComponents.sessionUseCases,
            webView)
    }

    override fun onStart() {
        super.onStart()

        sessionFeature?.start()
    }

    override fun onStop() {
        super.onStop()

        sessionFeature?.stop()
    }

    override fun onDestroyView() {
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onDestroyWebView(webView!!, session)

        super.onDestroyView()

        lifecycle.removeObserver(cursor!!)
        cursor = null

        sessionFeature = null
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity?.menuInflater?.inflate(R.menu.menu_context_hometile, menu)
    }

    // TODO: need new onBackPressed logic with OverlayFragment
    fun onBackPressed(): Boolean {
        if (session.canGoBack) {
            serviceLocator!!.sessionRepo.exitFullScreenIfPossibleAndBack() // TODO do this through WebRenderViewModel when it exists
            TelemetryIntegration.INSTANCE.browserBackControllerEvent()
            return true
        }
//        else -> {
//                context!!.webRenderComponents.sessionManager.remove()
//                return false
//            }
        return false
    }

    fun loadUrl(url: String) {
        if (url.isNotEmpty()) {
            val session = requireWebRenderComponents.sessionManager.selectedSession

            if (session != null) {
                // We already have an active session, let's just load the URL.
                requireWebRenderComponents.sessionUseCases.loadUrl.invoke(url)
            } else {
                // There's no session (anymore). Let's create a new one.
                requireWebRenderComponents.sessionManager.add(Session(url), selected = true)
            }
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
        return handleSpecialKeyEvent(event) ||
                (cursor?.keyDispatcher?.dispatchKeyEvent(event) ?: false)
    }

    private fun handleSpecialKeyEvent(event: KeyEvent): Boolean {
        val actionIsDown = event.action == KeyEvent.ACTION_DOWN

        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER && actionIsDown) MenuInteractionMonitor.selectPressed()

        if (session.isYoutubeTV &&
                event.keyCode == KeyEvent.KEYCODE_BACK) {
            val escKeyEvent = KeyEvent(event.action, KeyEvent.KEYCODE_ESCAPE)

            /**
             * If YouTube guide-list element is focused:
             * - Suppress the DOWN,BACK key event
             * - Change the UP,BACK key event to go back in history before YouTube
             * Else:
             * - Dispatch ESC key event
             */
            val jsCallback = ValueCallback<String> {
                if (it == "true") {
                    if (event.action == KeyEvent.ACTION_DOWN) Unit
                    else goBackBeforeYouTube()
                } else (activity as MainActivity).dispatchKeyEvent(escKeyEvent)
            }
            // This will return true if the currently focused YouTube element is a button in the left sidebar, false otherwise
            webView?.evalJS("""
                (function () {
                    return document.activeElement.parentElement.parentElement.id === 'guide-list';
                })();
                """,
                    jsCallback)
            return true
        }
        return false
    }

    private fun goBackBeforeYouTube() {
        val webView = (webView as ViewGroup).getChildAt(0) as WebView
        val backForwardUrlList = webView.copyBackForwardList().toList().map { it.originalUrl }
        val youtubeIndex = backForwardUrlList.lastIndexOf(URLs.YOUTUBE_TILE_URL)
        val goBackSteps = backForwardUrlList.size - youtubeIndex
        webView.goBackOrForward(-goBackSteps)
    }
}
