/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.NONE
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.channelsContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.navUrlInput
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.settingsTileContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_top_nav.*
import kotlinx.android.synthetic.main.hint_bar.hintBarContainer
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.experiments.ExperimentConfig
import org.mozilla.tv.firefox.ext.isKeyCodeSelect
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.hint.HintBinder
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.hint.InactiveHintViewModel
import org.mozilla.tv.firefox.channels.ChannelConfig
import org.mozilla.tv.firefox.channels.ChannelDetails
import org.mozilla.tv.firefox.channels.DefaultChannel
import org.mozilla.tv.firefox.channels.DefaultChannelFactory
import org.mozilla.tv.firefox.channels.SettingsChannelAdapter
import org.mozilla.tv.firefox.channels.SettingsScreen
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText
import java.lang.ref.WeakReference

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private val uiHandler = Handler(Looper.getMainLooper())

enum class NavigationEvent {
    BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION, DESKTOP_MODE, EXIT_FIREFOX, FXA_BUTTON,
    SETTINGS_DATA_COLLECTION, SETTINGS_CLEAR_COOKIES;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            R.id.desktopModeButton -> DESKTOP_MODE
            R.id.fxaButton -> FXA_BUTTON
            R.id.exitButton -> EXIT_FIREFOX
            else -> null
        }
    }
}

@Suppress("LargeClass")
class NavigationOverlayFragment : Fragment() {
    companion object {
        const val FRAGMENT_TAG = "overlay"
    }

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    private val uiLifecycleCancelJob: Job = Job()
    private val compositeDisposable = CompositeDisposable()

    // We need this in order to show the unpin toast, at max, once per
    // instantiation of the BrowserNavigationOverlay
    private var canShowUnpinToast: Boolean = false

    private val onNavigationEvent = { event: NavigationEvent, value: String?,
                                      autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? ->
        when (event) {
            NavigationEvent.LOAD_URL -> {
                (activity as MainActivity).onTextInputUrlEntered(value!!, autocompleteResult!!, UrlTextInputLocation.MENU)
                context?.serviceLocator?.screenController?.showNavigationOverlay(fragmentManager, false)
            }
            NavigationEvent.LOAD_TILE -> {
                (activity as MainActivity).onNonTextInputUrlEntered(value!!)
                context?.serviceLocator?.screenController?.showNavigationOverlay(fragmentManager, false)
            }
            NavigationEvent.SETTINGS_DATA_COLLECTION -> {
                serviceLocator.screenController.showSettingsScreen(fragmentManager!!, SettingsScreen.DATA_COLLECTION)
            }
            NavigationEvent.SETTINGS_CLEAR_COOKIES -> {
                serviceLocator.screenController.showSettingsScreen(fragmentManager!!, SettingsScreen.CLEAR_COOKIES)
            }

            // TODO: change button action based on profile state.
            // TODO: remember to update telemetry accordingly
            NavigationEvent.FXA_BUTTON -> {
                serviceLocator.fxaLoginUseCase.beginLogin(fragmentManager!!)
                TelemetryIntegration.INSTANCE.fxaButtonClickEvent()
            }

            NavigationEvent.TURBO, NavigationEvent.PIN_ACTION, NavigationEvent.DESKTOP_MODE, NavigationEvent.BACK,
            NavigationEvent.FORWARD, NavigationEvent.RELOAD, NavigationEvent.EXIT_FIREFOX -> { /* not handled by this object */ }
        }
        Unit
    }

    private lateinit var serviceLocator: ServiceLocator
    private lateinit var navigationOverlayViewModel: NavigationOverlayViewModel
    private lateinit var toolbarViewModel: ToolbarViewModel
    private lateinit var pocketViewModel: PocketViewModel
    private lateinit var hintViewModel: HintViewModel

    private var channelReferenceContainer: ChannelReferenceContainer? = null // references a Context, must be nulled.
    private val pinnedTileChannel: DefaultChannel get() = channelReferenceContainer!!.pinnedTileChannel
    private val pocketChannel: DefaultChannel get() = channelReferenceContainer!!.pocketChannel
    private val newsChannel: DefaultChannel get() = channelReferenceContainer!!.newsChannel
    private val sportsChannel: DefaultChannel get() = channelReferenceContainer!!.sportsChannel
    private val musicChannel: DefaultChannel get() = channelReferenceContainer!!.musicChannel

    private var rootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceLocator = context!!.serviceLocator

        navigationOverlayViewModel = FirefoxViewModelProviders.of(this).get(NavigationOverlayViewModel::class.java)
        toolbarViewModel = FirefoxViewModelProviders.of(this).get(ToolbarViewModel::class.java)
        pocketViewModel = FirefoxViewModelProviders.of(this).get(PocketViewModel::class.java)
        hintViewModel = if (serviceLocator.experimentsProvider.shouldShowHintBar()) {
            FirefoxViewModelProviders.of(this).get(OverlayHintViewModel::class.java)
        } else {
            InactiveHintViewModel()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_navigation_overlay_orig, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ToolbarUiController(
            toolbarViewModel,
            ::exitFirefox,
            onNavigationEvent
        ).onCreateView(view, viewLifecycleOwner, fragmentManager!!)

        rootView = view

        // TODO: Add back in once #1666 is ready to land.
        /*
        // Handle split overlay state on homescreen or webrender
        FirefoxViewModelProviders.of(this@NavigationOverlayFragment)
                .get(NavigationOverlayViewModel::class.java)
                .apply {
                    viewIsSplit.observe(viewLifecycleOwner, Observer { isSplit ->
                        isSplit ?: return@Observer
                        val windowSpacerHeight = if (isSplit) OVERLAY_SPACER_WEBRENDER_HEIGHT else OVERLAY_SPACER_HOMESCREEN_HEIGHT
                        overlayWindowSpacer.apply {
                            layoutParams.height = windowSpacerHeight
                            requestLayout()
                        }
                        navOverlayScrollView.scrollY = 0
                    })
                }
                */

        initSettingsChannel() // When pulling everything into channels, add this to the channel RV

        exitButton.contentDescription = serviceLocator.experimentsProvider.getAAExitButtonExperiment(ExperimentConfig.AA_TEST)

        val tintDrawable: (Drawable?) -> Unit = { it?.setTint(ContextCompat.getColor(context!!, R.color.photonGrey10_a60p)) }
        navUrlInput.compoundDrawablesRelative.forEach(tintDrawable)
        registerForContextMenu(channelsContainer)
        canShowUnpinToast = true

        channelReferenceContainer = ChannelReferenceContainer(channelsContainer, createChannelFactory()).also {
            channelsContainer.addView(it.pocketChannel.channelContainer)
            channelsContainer.addView(it.pinnedTileChannel.channelContainer)
            channelsContainer.addView(it.newsChannel.channelContainer)
            channelsContainer.addView(it.sportsChannel.channelContainer)
            channelsContainer.addView(it.musicChannel.channelContainer)
        }
    }

    override fun onStart() {
        super.onStart()
        observePinnedTiles()
            .addTo(compositeDisposable)
        observeTileRemoval()
            .forEach { compositeDisposable.add(it) }
        observeRequestFocus()
                .addTo(compositeDisposable)
        observeChannelVisibility()
            .forEach { compositeDisposable.add(it) }
        observePocket()
            .forEach { compositeDisposable.add(it) }
        observeTvGuideTiles()
            .forEach { compositeDisposable.add(it) }
        HintBinder.bindHintsToView(hintViewModel, hintBarContainer, animate = false)
                .forEach { compositeDisposable.add(it) }
        observeToolbarFocusability()
                .addTo(compositeDisposable)

        fxaButton.isVisible = serviceLocator.experimentsProvider.shouldShowSendTab()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    fun dispatchKeyEvent(
        event: KeyEvent,
        @VisibleForTesting(otherwise = NONE) menuInteractionMonitor: MenuInteractionMonitor = MenuInteractionMonitor
    ): Boolean {
        // MenuInteractionMonitor broke, which went unnoticed for several releases, when the overlay was refactored into
        // a different Fragment: it might be safer to model this reactively, in our architecture, which abstracts away
        // such framework constructs.
        if (event.isKeyCodeSelect && event.action == KeyEvent.ACTION_DOWN) {
            menuInteractionMonitor.selectPressed()
        }

        return false
    }

    private fun exitFirefox() {
        activity!!.moveTaskToBack(true)
    }

    private fun observePinnedTiles(): Disposable {
        return navigationOverlayViewModel.pinnedTiles.subscribe {
            pinnedTileChannel.setTitle(it.title)
            pinnedTileChannel.setContents(it.tileList)
        }
    }

    private fun observeTileRemoval(): List<Disposable> {
        fun DefaultChannel.forwardRemoveEventsToRepo(): Disposable =
            this.removeTileEvents
                .subscribe { tileToRemove ->
                    serviceLocator.channelRepo.removeChannelContent(tileToRemove)
                }

        return listOf(
            pinnedTileChannel,
            newsChannel,
            sportsChannel,
            musicChannel
        ).map { channel -> channel.forwardRemoveEventsToRepo() }
    }

    private fun observeToolbarFocusability(): Disposable {
        return navigationOverlayViewModel.leftmostActiveToolBarId
                .subscribe { leftmostToolbarId ->
                    // Reset previous left most active toolbar button's nextFocusLeftID
                    rootView?.findViewById<View>(navUrlInput.nextFocusUpId)?.nextFocusLeftId = -1
                    // Disable left direction click on leftmostToolbarId
                    rootView?.findViewById<View>(leftmostToolbarId)?.nextFocusLeftId = leftmostToolbarId

                    navUrlInput.nextFocusUpId = leftmostToolbarId
                }
    }

    private fun observeChannelVisibility(): List<Disposable> {
        // NOTE: for unknown reasons, removing the last tile in a channel crashes if it is visible.
        // If you change this method, please be sure to test that case.
        //
        // We considered making channel visibility part of DefaultChannel's behavior, but were
        // concerned about a potential problem.
        //
        // Assume that we handle isEmpty visibility automatically, somewhere in Channel or
        // DefaultChannelFactory. Then we get a requirement to set visibility some other way
        // (maybe the user can hide the tile). We could get easy to miss, bad interactions where
        // our viewmodel sets visibility in one way, but the view itself has other behavior. For
        // now, we have chosen to make this visibility change explicit, and the responsibility of
        // the dev who is adding a new channel. We may revisit this in the future.
        fun observeVisibility(details: Observable<ChannelDetails>, channel: DefaultChannel): Disposable =
            navigationOverlayViewModel.shouldBeDisplayed(details).subscribe { shouldDisplay ->
                channel.channelContainer.isVisible = shouldDisplay
            }

        return listOf(
            observeVisibility(navigationOverlayViewModel.pinnedTiles, pinnedTileChannel),
            observeVisibility(navigationOverlayViewModel.newsChannel, newsChannel),
            observeVisibility(navigationOverlayViewModel.sportsChannel, sportsChannel),
            observeVisibility(navigationOverlayViewModel.musicChannel, musicChannel)
        )
    }
    private fun observeRequestFocus(): Disposable {
        return navigationOverlayViewModel.focusView
                .subscribe { viewToFocus ->
                    rootView?.findViewById<View>(viewToFocus)?.requestFocus()
                }
    }

    private fun observePocket(): List<Disposable> {
        val disposables = mutableListOf<Disposable>()
        disposables += observePocketVisibility()
        disposables += observePocketTiles()

        return disposables
    }

    private fun observePocketVisibility(): Disposable = pocketViewModel.state
            .subscribe { when (it) {
                is PocketViewModel.State.Feed -> pocketChannel.channelContainer.visibility = View.VISIBLE
                else -> pocketChannel.channelContainer.visibility = View.GONE
            } }

    private fun observePocketTiles(): Disposable {
        val pocketDetails = pocketViewModel.state
            .ofType(PocketViewModel.State.Feed::class.java)
            .map { it.details }
        return defaultObserveChannelDetails(pocketChannel, pocketDetails)
    }

    private fun observeTvGuideTiles(): List<Disposable> {
        return listOf(
            defaultObserveChannelDetails(newsChannel, navigationOverlayViewModel.newsChannel),
            defaultObserveChannelDetails(sportsChannel, navigationOverlayViewModel.sportsChannel),
            defaultObserveChannelDetails(musicChannel, navigationOverlayViewModel.musicChannel)
        )
    }

    private fun defaultObserveChannelDetails(channel: DefaultChannel, source: Observable<ChannelDetails>) =
        source.subscribe {
            channel.setTitle(it.title)
            channel.setSubtitle(it.subtitle)
            channel.setContents(it.tileList)
        }

    private fun createChannelFactory(): DefaultChannelFactory = DefaultChannelFactory(
            loadUrl = { urlStr ->
                if (urlStr.isNotEmpty()) {
                    onNavigationEvent.invoke(NavigationEvent.LOAD_TILE, urlStr, null)
                }
            },
            onTileFocused = {
                val prefInt = android.preference.PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        SHOW_UNPIN_TOAST_COUNTER_PREF, 0)
                if (prefInt < MAX_UNPIN_TOAST_COUNT && canShowUnpinToast) {
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putInt(SHOW_UNPIN_TOAST_COUNTER_PREF, prefInt + 1)
                            .apply()

                    val contextReference = WeakReference(context)
                    val showToast = showToast@{
                        val context = contextReference.get() ?: return@showToast
                        Toast.makeText(context, R.string.homescreen_unpin_tutorial_toast,
                                android.widget.Toast.LENGTH_LONG).show()
                    }
                    // We believe this delays in order to avoid speaking over the focus
                    // change announcement. However this is taken legacy code, so there
                    // may be other reasons as well
                    if (context!!.isVoiceViewEnabled()) uiHandler.postDelayed(showToast, 1500)
                    else showToast.invoke()

                    canShowUnpinToast = false
                }
            }
    )

    private fun initSettingsChannel() {
        settingsTileContainer.gridView.adapter = SettingsChannelAdapter(
                loadUrl = { urlStr ->
                    onNavigationEvent.invoke(NavigationEvent.LOAD_TILE, urlStr, null)
                },
                showSettings = { type ->
                    val navigationEvent = when (type) {
                        SettingsScreen.DATA_COLLECTION -> NavigationEvent.SETTINGS_DATA_COLLECTION
                        SettingsScreen.CLEAR_COOKIES -> NavigationEvent.SETTINGS_CLEAR_COOKIES
                    }
                    onNavigationEvent.invoke(navigationEvent, null, null)
                }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null

        // Since we start the async jobs in View.init and Android is inflating the view for us,
        // there's no good way to pass in the uiLifecycleJob. We could consider other solutions
        // but it'll add complexity that I don't think is probably worth it.
        uiLifecycleCancelJob.cancel()
        channelReferenceContainer = null
    }
}

/**
 * A [ScrollView] with functionality overridden for the specific requirements of the overlay.
 *
 * One crappy thing with the current implementation is that when a scroll is interrupted (e.g. user
 * clicks up twice quickly), it will skip and not scroll smoothly. Since we don't scroll often,
 * this seems fine.
 */
private const val OVERLAY_SPACER_HOMESCREEN_HEIGHT = 393
private const val OVERLAY_SPACER_WEBRENDER_HEIGHT = 800
class BrowserNavigationOverlayScrollView(
    context: Context,
    attrs: AttributeSet
) : NestedScrollView(context, attrs) {

    private val deltaScrollPadding = resources.getDimensionPixelSize(R.dimen.browser_overlay_delta_scroll_padding)

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int {
        // We modify the scroll offset to ensure:
        // 1) Scrolling through the tiles will show enough of the next tile to indicate scrollability.
        // 2) When focusing the last vertical view in the layout, the default implementation will
        //    leave some empty space at the edge of the view such that an additional dpad click will
        //    scroll the screen but nothing new is focused: we don't want that.
        val deltaScrollForOnScreen = super.computeScrollDeltaToGetChildRectOnScreen(rect)
        return deltaScrollForOnScreen + deltaScrollPadding * Integer.signum(deltaScrollForOnScreen)
    }
}

/**
 * A data container for references to home channels. This object references a Context: it must be nulled when its
 * lifecycle ends.
 *
 * This class exists to group together all of the [DefaultChannel]s - which reference a [Context] - so that we can
 * null all of them in one statement and not need to remember to null references to newly added [DefaultChannel]s.
 */
private class ChannelReferenceContainer(
    channelContainerView: ViewGroup,
    channelFactory: DefaultChannelFactory
) {

    val pocketChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.pocket_channel,
        channelConfig = ChannelConfig.getPocketConfig(channelContainerView.context)
    )

    val pinnedTileChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.pinned_tiles_channel,
        channelConfig = ChannelConfig.getPinnedTileConfig(channelContainerView.context)
    )

    val newsChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.news_channel,
        channelConfig = ChannelConfig.getTvGuideConfig(channelContainerView.context)
    )

    val sportsChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.sports_channel,
        channelConfig = ChannelConfig.getTvGuideConfig(channelContainerView.context)
    )

    val musicChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.music_channel,
        channelConfig = ChannelConfig.getTvGuideConfig(channelContainerView.context)
    )
}
