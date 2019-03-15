/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.UiThread
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_browser.view.browserFragmentRoot
import kotlinx.android.synthetic.main.fragment_browser.view.cursorView
import kotlinx.android.synthetic.main.fragment_browser.view.engineView
import kotlinx.android.synthetic.main.fragment_browser.view.progressBar
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.feature.session.SessionFeature
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.MediaSessionHolder
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.architecture.FocusOnShowDelegate
import org.mozilla.tv.firefox.ext.focusedDOMElement
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.isYoutubeTV
import org.mozilla.tv.firefox.ext.pauseAllVideoPlaybacks
import org.mozilla.tv.firefox.ext.requireWebRenderComponents
import org.mozilla.tv.firefox.ext.resetView
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.webrender.cursor.CursorController
import org.mozilla.tv.firefox.webrender.cursor.CursorViewModel

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
    private val compositeDisposable = CompositeDisposable()

    /**
     * Encapsulates the cursor's components. If this value is null, the Cursor is not attached
     * to the view hierarchy.
     */
    var cursor: CursorController? = null
        @UiThread get set // Set from the UI thread so serial access is required for simplicity.

    var sessionFeature: SessionFeature? = null
    // If YouTubeBackHandler is instantiated without an EngineView, YouTube won't
    // work properly, so we !!
    private val youtubeBackHandler by lazy { YouTubeBackHandler(engineView!!, activity as MainActivity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        super.onResume()
        if (session.isYoutubeTV) {
            YoutubeGreyScreenWorkaround.invoke(activity)
        }
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

    override fun onUrlChanged(session: Session, url: String) {
        if (url == URLs.APP_URL_HOME) serviceLocator?.screenController?.showNavigationOverlay(fragmentManager, true)
        youtubeBackHandler.onUrlChanged(url)
    }

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        if (!loading) youtubeBackHandler.onLoadComplete()
    }

    override fun onDesktopModeChanged(session: Session, enabled: Boolean) {
        requireWebRenderComponents.sessionUseCases.requestDesktopSite.invoke(enabled, session)
    }

    override fun onContentPermissionRequested(session: Session, permissionRequest: PermissionRequest): Boolean =
        permissionRequest.grantIf { it is Permission.ContentProtectedMediaId }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = inflater.context
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        val viewModelFactory = context.serviceLocator.viewModelFactory
        cursor = CursorController.newInstanceOnCreateView(
            this,
            cursorParent = layout.browserFragmentRoot,
            view = layout.cursorView,
            viewModel = ViewModelProviders.of(this, viewModelFactory).get(CursorViewModel::class.java)
        ).also {
            lifecycle.addObserver(it)
        }

        layout.progressBar.initialize(this)

        // We break encapsulation here: we should use the super.engineView reference but it's not init until
        // onViewCreated. However, overriding both onCreateView and onViewCreated in a single class
        // is confusing so I'd rather break encapsulation than confuse devs.
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onCreateEngineView(layout.engineView, session)

        return layout
    }

    override fun onEngineViewCreated(engineView: EngineView) {
        // The SessionFeature implementation will take care of making sure that we always render the currently selected
        // session in our engine view.
        sessionFeature = SessionFeature(
            requireWebRenderComponents.sessionManager,
            requireWebRenderComponents.sessionUseCases,
            engineView)

        serviceLocator?.screenController?.currentActiveScreen?.observe(viewLifecycleOwner, Observer {
            if (it == ActiveScreen.WEB_RENDER) {
                // Cache focused DOM element just before WebView gains focus. See comment in
                // FocusedDOMElementCacheInterface for details
                engineView.focusedDOMElement.cache()
            } else {
                // Pause all the videos when transitioning out of [WebRenderFragment] to mitigate possible
                // memory leak while clearing data. See [WebViewCache.clear] as well as #1720
                engineView.pauseAllVideoPlaybacks()
            }
        })
    }

    override fun onStart() {
        super.onStart()

        sessionFeature?.start()
        serviceLocator!!.sessionRepo.events.subscribe {
            when (it) {
                SessionRepo.Event.YouTubeBack -> youtubeBackHandler.onBackPressed()
                SessionRepo.Event.ExitYouTube -> youtubeBackHandler.goBackBeforeYouTube()
                // Rx will never emit a null, but the compiler doesn't believe me
                null -> return@subscribe
            }.forceExhaustive
        }.addTo(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()

        serviceLocator!!.sessionRepo.exitFullScreenIfPossible()
        sessionFeature?.stop()
        compositeDisposable.clear()
    }

    override fun onDestroyView() {
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onDestroyEngineView(engineView!!, session)

        super.onDestroyView()

        lifecycle.removeObserver(cursor!!)
        cursor = null

        sessionFeature = null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        FocusOnShowDelegate().onHiddenChanged(this, hidden)
        super.onHiddenChanged(hidden)
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
                requireWebRenderComponents.sessionManager.getOrCreateEngineSession().resetView(activity!!)
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

        // TODO move this (into NavigationOverlayFragment?)
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER && actionIsDown) MenuInteractionMonitor.selectPressed()

        return false
    }
}
