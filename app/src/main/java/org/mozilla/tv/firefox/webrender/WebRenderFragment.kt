/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isGone
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.android.synthetic.main.hint_bar.*
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.feature.session.SessionFeature
import mozilla.components.support.ktx.android.util.dpToPx
import mozilla.components.support.ktx.android.view.use
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.MediaSessionHolder
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.ext.addSubmitListenerToInputElements
import org.mozilla.tv.firefox.ext.couldScrollInDirection
import org.mozilla.tv.firefox.ext.focusedDOMElement
import org.mozilla.tv.firefox.ext.isUrlWhitelistedForSubmitInputHack
import org.mozilla.tv.firefox.ext.isYoutubeTV
import org.mozilla.tv.firefox.ext.maybeGoBackBeforeFxaSignIn
import org.mozilla.tv.firefox.ext.observeScrollPosition
import org.mozilla.tv.firefox.ext.pauseAllVideoPlaybacks
import org.mozilla.tv.firefox.ext.requireWebRenderComponents
import org.mozilla.tv.firefox.ext.resetView
import org.mozilla.tv.firefox.ext.scrollByClamped
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.updateFullscreenScrollPosition
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.hint.HintBinder
import org.mozilla.tv.firefox.hint.InactiveHintViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.SupportUtils
import org.mozilla.tv.firefox.utils.URLs

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

    private val startStopCompositeDisposable = CompositeDisposable()

    // If YouTubeBackHandler is instantiated without an EngineView, YouTube won't
    // work properly, so we !!
    private val youtubeBackHandler by lazy { YouTubeBackHandler(engineView!!, activity as MainActivity) }

    private lateinit var webRenderViewModel: WebRenderViewModel
    private var rootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSession()

        webRenderViewModel = FirefoxViewModelProviders.of(this).get(WebRenderViewModel::class.java)
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

        if (enabled &&
                serviceLocator?.experimentsProvider?.shouldUseMp4VideoWorkaround() == true) {
            engineView?.updateFullscreenScrollPosition()
        }

        val bannerLayout: View = window.findViewById(R.id.bannerLayout)
        bannerLayout.isGone = enabled
    }

    override fun onUrlChanged(session: Session, url: String) {
        if (url == URLs.APP_URL_HOME) serviceLocator?.screenController?.showNavigationOverlay(fragmentManager, true)
        youtubeBackHandler.onUrlChanged(url)
    }

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        if (!loading) {
            // If the page isn't finished loading, our observers won't be attached to capture the scroll position
            // and the fix won't work. Unfortunately, I've spent too much time on this so I did not prepare a fix.
            engineView?.observeScrollPosition()

            if (session.url.isUrlWhitelistedForSubmitInputHack) {
                engineView?.addSubmitListenerToInputElements()
            }

            youtubeBackHandler.onLoadComplete()
        }
    }

    override fun onDesktopModeChanged(session: Session, enabled: Boolean) {
        requireWebRenderComponents.sessionUseCases.requestDesktopSite.invoke(enabled, session)
    }

    override fun onContentPermissionRequested(session: Session, permissionRequest: PermissionRequest): Boolean =
        permissionRequest.grantIf { it is Permission.ContentProtectedMediaId }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = inflater.context
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        layout.browserFragmentRoot.addOnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
            context.serviceLocator.cursorModel.screenBounds = PointF(right.toFloat(), bottom.toFloat())
        }
        context.serviceLocator.cursorModel.webViewCouldScrollInDirectionProvider = layout.engineView::couldScrollInDirection

        // Setup the banner

        val bannerLayout: View = layout.findViewById(R.id.bannerLayout)

        val moreInfoButton: Button = bannerLayout.findViewById(R.id.bannerMoreInfoButton)
        moreInfoButton.setOnClickListener {
            (activity as MainActivity).onNonTextInputUrlEntered(SupportUtils.getSumoURLForTopic(this.context, "amazon-end-support"))
            context?.serviceLocator?.screenController?.showNavigationOverlay(fragmentManager, false)
        }

        layout.progressBar.initialize(this)

        // We break encapsulation here: we should use the super.engineView reference but it's not init until
        // onViewCreated. However, overriding both onCreateView and onViewCreated in a single class
        // is confusing so I'd rather break encapsulation than confuse devs.
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onCreateEngineView(layout.engineView, session)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
    }

    // TODO: this method needs to be renamed (#2053); preliminary onStart() setup
    override fun onEngineViewCreated(engineView: EngineView): Disposable? {
        return serviceLocator?.screenController?.currentActiveScreen?.subscribe {
            if (it != ActiveScreen.WEB_RENDER) {
                // Pause all the videos when transitioning out of [WebRenderFragment] to mitigate possible
                // memory leak while clearing data. See [WebViewCache.clear] as well as #1720
                engineView.pauseAllVideoPlaybacks()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        observeRequestFocus()
                .addTo(startStopCompositeDisposable)
        observeFxaLoginSuccess()
                .addTo(startStopCompositeDisposable)

        /**
         * When calling getOrCreateEngineSession(), [SessionManager] lazily creates an [EngineSession]
         * instance and links it with its respective [Session]. During the linking, [SessionManager]
         * calls EngineSession.loadUrl(session.url), which, during initialization, is Session.initialUrl
         *
         * This is how "about:home" successfully gets added to [WebView.WebForwardList], with which
         * we do various different operations (such as exiting the app and handling Youtube back)
         *
         * We need to manually reload the session.url since we are replacing the webview instance that
         * has already called loadUrl(session.url) during [EngineView] lazy instantiation
         *
         * [SessionFeature.start] would eventually call [EngineView.render] which then initializes
         * its associated [EngineSession.webview]. We need make sure to load initialUrl after
         * WebView sets its WebViewClient (which happens during EngineView.render())
         */
        requireWebRenderComponents.sessionManager.getOrCreateEngineSession().loadUrl(session.url)
        serviceLocator!!.sessionRepo.events.subscribe {
            when (it) {
                SessionRepo.Event.YouTubeBack -> youtubeBackHandler.onBackPressed()
                SessionRepo.Event.ExitYouTube -> youtubeBackHandler.goBackBeforeYouTube()
                // Rx will never emit a null, but the compiler doesn't believe me
                null -> return@subscribe
            }
        }.addTo(startStopCompositeDisposable)

        serviceLocator!!.cursorModel.scrollRequests
                .subscribe { engineView!!.scrollByClamped(it.x.toInt(), it.y.toInt()) }
                .addTo(startStopCompositeDisposable)

        cursorView.setup(context!!.serviceLocator.cursorModel)
                .addTo(startStopCompositeDisposable)

        val (hintViewModel, progressBarBottomMargin) = if (serviceLocator!!.experimentsProvider.shouldShowHintBar()) {
            FirefoxViewModelProviders.of(this).get(WebRenderHintViewModel::class.java) to
                64.dpToPx(resources.displayMetrics)
        } else {
            InactiveHintViewModel() to 0
        }

        (progressBar.layoutParams as? FrameLayout.LayoutParams)?.bottomMargin = progressBarBottomMargin

        HintBinder.bindHintsToView(hintViewModel, hintBarContainer, animate = true)
                .forEach { startStopCompositeDisposable.add(it) }
    }

    override fun onStop() {
        super.onStop()

        serviceLocator!!.sessionRepo.exitFullScreenIfPossible()
        startStopCompositeDisposable.clear()
    }

    override fun onDestroyView() {
        mediaSessionHolder?.videoVoiceCommandMediaSession?.onDestroyEngineView(engineView!!, session)

        context!!.serviceLocator.cursorModel.webViewCouldScrollInDirectionProvider = null

        rootView = null

        super.onDestroyView()
    }

    private fun observeRequestFocus(): Disposable {
        // EngineView focus may be lost after waking up from sleep & screen saver.
        // Forcibly request focus onStart(), after DOMElement cache, IFF webRenderFragment
        // is the current ActiveScreen
        return webRenderViewModel.focusRequests
                .subscribe { viewId ->
                    rootView?.findViewById<View>(viewId).let { viewToFocus ->
                        // Cache focused DOM element just before WebView gains focus. See comment in
                        // FocusedDOMElementCacheInterface for details
                        (viewToFocus as EngineView).focusedDOMElement.cache()
                        viewToFocus.requestFocus()
                    }
                }
    }

    private fun observeFxaLoginSuccess(): Disposable {
        return webRenderViewModel.onFxaLoginSuccess.subscribe {
            engineView?.maybeGoBackBeforeFxaSignIn()
        }
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
        fun handleCursorKeyEvent(event: KeyEvent): Boolean {
            val handledCursorEvent = serviceLocator?.cursorModel?.handleKeyEvent(event)
            handledCursorEvent?.simulatedTouch?.use {
                activity?.dispatchTouchEvent(it)
            }
            return handledCursorEvent?.wasKeyEventConsumed == true
        }

        return handleCursorKeyEvent(event)
    }
}
