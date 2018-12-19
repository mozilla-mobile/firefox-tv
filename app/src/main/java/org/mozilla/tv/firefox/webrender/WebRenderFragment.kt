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
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.utils.URLs

private const val ARGUMENT_SESSION_UUID = "sessionUUID"

/**
 * Fragment for displaying the browser UI.
 */
@Suppress("LargeClass") // TODO this will be removed as part of the upcoming architecture refactor
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

    private val isUrlEqualToHomepage: Boolean get() = session.url == URLs.APP_URL_HOME

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

    // TODO: need to find a workaround for NavigationOverlayFragment
//    override fun onUrlChanged(session: Session, url: String) {
//        if (url == AppConstants.APP_URL_POCKET_ERROR) {
//            browserOverlay?.showMegaTileError() // TODO remove and verify that the Pocket refactor handles this
//            browserOverlay?.visibility = View.VISIBLE
//        }
//
//        updateOverlayIfVisible()
//    }

//    override fun onLoadingStateChanged(session: Session, loading: Boolean) = updateOverlayIfVisible()
//
//    override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) =
//        updateOverlayIfVisible()

    override fun onFullScreenChanged(session: Session, enabled: Boolean) {
        val window = (context as? Activity)?.window ?: return
        val dontSleep = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (enabled) window.addFlags(dontSleep)
        else window.clearFlags(dontSleep)
    }

//    private val onNavigationEvent = { event: NavigationEvent, value: String?,
//                                      autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? ->
//        when (event) {
//            NavigationEvent.SETTINGS -> serviceLocator.screenController.showSettingsScreen(fragmentManager!!)
//            NavigationEvent.LOAD_URL -> {
//                (activity as MainActivity).onTextInputUrlEntered(value!!, autocompleteResult!!, UrlTextInputLocation.MENU)
//                setOverlayVisible(false)
//            }
//            NavigationEvent.LOAD_TILE -> {
//                (activity as MainActivity).onNonTextInputUrlEntered(value!!)
//                setOverlayVisible(false)
//            }
//            NavigationEvent.POCKET -> {
//                val (fragmentManager, activity) = Pair(fragmentManager, activity)
//                if (fragmentManager != null && activity != null) {
//                    serviceLocator.screenController.showPocketScreen(fragmentManager)
//                }
//            }
//            NavigationEvent.TURBO, NavigationEvent.PIN_ACTION, NavigationEvent.DESKTOP_MODE, NavigationEvent.BACK,
//            NavigationEvent.FORWARD, NavigationEvent.RELOAD -> { /* not handled by this object */ }
//        }
//        Unit
//    }

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

        /**
         * TODO: #1085
         * When navigating through fragment backstack from MainActivity.onBackPressed(),
         * FragmentManager.transaction recreates BrowserFragment which results in
         * browserOverlay.setVisibility to be called. For now, we must update browserOverlay's
         * parent state in onCreateView due to inconsistency in the existence of the browserOverlay
         * instance elsewhere in the fragment lifecycle (refer to Issue #1107) and the timing from
         * which browserOverlay.setVisibility is called.
         */
//        val bundle: Bundle? = arguments
//
//        with(layout.browserOverlay) {
//            if (bundle?.getSerializable(PARENT_FRAGMENT) != null) {
//                parentFrag = bundle.getSerializable(PARENT_FRAGMENT) as BrowserNavigationOverlay.ParentFragment
//            }
//
//            // FIXME: Need [WebRenderFragment] as lifeCycle owner until NavOverlayFragment breakout
//            pinnedTileViewModel = this@WebRenderFragment.pinnedTileViewModel
//            lifeCycleOwner = this@WebRenderFragment.viewLifecycleOwner
//            initPinnedTiles()
//            observeForMegaTile(this@WebRenderFragment)
//            observeForToolbar(this@WebRenderFragment)
//
//            onNavigationEvent = this@WebRenderFragment.onNavigationEvent
//            setOverlayVisible = { visible -> setOverlayVisible(visible) }
//            visibility = overlayVisibleCached ?: View.GONE
//
//            // This is needed for YouTube to properly gain focus after a refresh (refer to issue #1149)
//            onPreSetVisibilityListener = { isVisible ->
//                // The overlay can clear the DOM and a previous focused element cache (e.g. reload)
//                // so we need to do our own caching: see FocusedDOMElementCacheInterface for details.
//                if (!isVisible) { webView?.focusedDOMElement?.cache() }
//            }
//
//            openHomeTileContextMenu = {
//                activity?.openContextMenu(browserOverlay.tileContainer)
//            }
//
//            registerForContextMenu(browserOverlay.tileContainer)
//        }

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


//        if (session.url == AppConstants.APP_URL_HOME) { //TODO: need a workaround for NavOverlayFrag
//            browserOverlay?.visibility = View.VISIBLE
//        }
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
//        when {
//            browserOverlay.isVisible && !isUrlEqualToHomepage -> {
//                setOverlayVisible(false)
//                TelemetryIntegration.INSTANCE.userShowsHidesDrawerEvent(false)
//            }
//            session.canGoBack -> {
//                serviceLocator.sessionRepo.exitFullScreenIfPossibleAndBack() // TODO do this through WebRenderViewModel when it exists
//                TelemetryIntegration.INSTANCE.browserBackControllerEvent()
//            }
//            else -> {
//                context!!.webRenderComponents.sessionManager.remove()
//                return false
//            }
//        }
        return true
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
//            setOverlayVisible(false) TODO: show overlayFragment (backpress?)
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

        if (event.keyCode == KeyEvent.KEYCODE_MENU && !isUrlEqualToHomepage) {
            if (actionIsDown) {
                // TODO: show overlayFragment
//                val toShow = !browserOverlay.isVisible
//                setOverlayVisible(toShow)
//                TelemetryIntegration.INSTANCE.userShowsHidesDrawerEvent(toShow)
            }
            return true
        }

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

//    /**
//     * Changes the overlay visibility: this should be called instead of changing
//     * [BrowserNavigationOverlay.isVisible] directly.
//     */
//    private fun setOverlayVisible(toShow: Boolean) {
//        if (browserOverlay.parentFrag != BrowserNavigationOverlay.ParentFragment.DEFAULT) {
//            browserOverlay.parentFrag = BrowserNavigationOverlay.ParentFragment.DEFAULT
//        }
//
//        browserOverlay.visibility = if (toShow) View.VISIBLE else View.GONE
//        if (toShow) cursor?.onPause() else cursor?.onResume()
//        cursor?.setEnabledForCurrentState()
//        if (toShow) MenuInteractionMonitor.menuOpened() else MenuInteractionMonitor.menuClosed()
//        // TODO once the overlay is a separate fragment, handle PocketRepoCache changes in ScreenController
//        val pocketRepoCache = serviceLocator.pocketRepoCache
//        if (toShow) pocketRepoCache.freeze() else pocketRepoCache.unfreeze()
//    }
}
