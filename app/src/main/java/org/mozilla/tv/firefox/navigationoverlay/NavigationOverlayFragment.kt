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
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.android.synthetic.main.default_channel.view.*
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.channelsContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.navUrlInput
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.settingsTileContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_top_nav.exitButton
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
import org.mozilla.tv.firefox.navigationoverlay.channels.ChannelConfig
import org.mozilla.tv.firefox.navigationoverlay.channels.DefaultChannel
import org.mozilla.tv.firefox.navigationoverlay.channels.DefaultChannelFactory
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsChannelAdapter
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsScreen
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.pocket.toChannelTiles
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private val uiHandler = Handler(Looper.getMainLooper())

enum class NavigationEvent {
    BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION, DESKTOP_MODE, EXIT_FIREFOX,
    SETTINGS_DATA_COLLECTION, SETTINGS_CLEAR_COOKIES;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            R.id.desktopModeButton -> DESKTOP_MODE
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
        }
    }

    override fun onStart() {
        super.onStart()
        observeFocusState()
            .addTo(compositeDisposable)
        observeRequestFocus()
            .addTo(compositeDisposable)
        observePinnedTiles()
            .addTo(compositeDisposable)
        observePinnedTileRemoval()
            .addTo(compositeDisposable)
        observeShouldDisplayPinnedTiles()
            .addTo(compositeDisposable)
        observePocket()
            .forEach { compositeDisposable.add(it) }
        HintBinder.bindHintsToView(hintViewModel, hintBarContainer, animate = false)
                .forEach { compositeDisposable.add(it) }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // MenuInteractionMonitor broke, which went unnoticed for several releases, when the overlay was refactored into
        // a different Fragment: it might be safer to model this reactively, in our architecture, which abstracts away
        // such framework constructs.
        if (event.isKeyCodeSelect && event.action == KeyEvent.ACTION_DOWN) {
            MenuInteractionMonitor.selectPressed()
        }

        return false
    }

    private fun exitFirefox() {
        activity!!.moveTaskToBack(true)
    }

    /**
     * observeRequestFocus() handles screen transition request focus
     */
    private fun observeRequestFocus(): Disposable {
        return navigationOverlayViewModel.focusRequests
            .subscribe { viewId ->
                val viewToFocus = rootView?.findViewById<View>(viewId)
                viewToFocus?.requestFocus()
            }
    }

    private fun observeFocusState(): Disposable {
        return navigationOverlayViewModel.focusUpdate
            .subscribe { focusState ->
                rootView?.findViewById<View>(focusState.focusNode.viewId)?.let { focusedView ->
                    focusState.focusNode.updateViewNodeTree(focusedView)
                    if (!focusState.focused)
                        focusedView.requestFocus()
                }
            }
    }

    private fun observePinnedTiles(): Disposable {
        return navigationOverlayViewModel.pinnedTiles.subscribe {
            pinnedTileChannel.setTitle(it.title)
            pinnedTileChannel.setContents(it.tileList)
        }
    }

    private fun observePinnedTileRemoval(): Disposable {
        return pinnedTileChannel.removeTileEvents.subscribe { tileToRemove ->
            serviceLocator.channelRepo.removeChannelContent(tileToRemove)
        }
    }

    private fun observeShouldDisplayPinnedTiles(): Disposable {
        // We considered putting all common channel behavior into one reusable function (and may
        // still in the future), but were concerned about a potential problem.
        //
        // Assume that we handle isEmpty visibility automatically, somewhere in Channel or
        // DefaultChannelFactory. Then we get a requirement to set visibility some other way
        // (maybe the user can hide the tile). We could get easy to miss, bad interactions where
        // our viewmodel sets visibility in one way, but the view itself has other behavior. For
        // now, we have chosen to make this visibility change explicit, and the responsibility of
        // the dev who is adding a new channel. We may revisit this in the future.
        return navigationOverlayViewModel.shouldDisplayPinnedTiles.subscribe { shouldDisplay ->
            pinnedTileChannel.channelContainer.visibility = when (shouldDisplay) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        }
    }

    private fun observePocket(): List<Disposable> {
        setPocketTextVisibility()

        val disposables = mutableListOf<Disposable>()
        disposables += observePocketVisibility()
        disposables += observePocketTiles()
        disposables += observePocketDescriptions()
        disposables += observePocketFocusLoss()

        return disposables
    }

    private fun setPocketTextVisibility() {
        pocketChannel.channelContainer.channelBelowText1.visibility = View.VISIBLE
        pocketChannel.channelContainer.channelBelowText2.visibility = View.VISIBLE
    }

    private fun observePocketVisibility(): Disposable = pocketViewModel.state
            .subscribe { when (it) {
                is PocketViewModel.State.Feed -> pocketChannel.channelContainer.visibility = View.VISIBLE
                else -> pocketChannel.channelContainer.visibility = View.GONE
            } }

    private fun observePocketTiles(): Disposable = pocketViewModel.state
            .ofType(PocketViewModel.State.Feed::class.java)
            .map { it.feed.toChannelTiles() }
            .subscribe {
                pocketChannel.setTitle(context!!.resources.getString(R.string.pocket_channel_title))
                pocketChannel.setContents(it)
            }

    /**
     * Sets description text beneath the Pocket channel based on the currently focused tile
     */
    private fun observePocketDescriptions(): Disposable = pocketChannel.focusChangeObservable
            .filter { (_, focusGained) -> focusGained }
            .map { (index, _) -> index }
            .withLatestFrom(pocketViewModel.state)
            .subscribe { (index, pocketState) ->
                val data = pocketState as? PocketViewModel.State.Feed ?: return@subscribe
                val focusedData = data.feed[index] as? PocketViewModel.FeedItem.Video
                        ?: return@subscribe
                val topText = pocketChannel.channelContainer.channelBelowText1
                val bottomText = pocketChannel.channelContainer.channelBelowText2

                topText.text = focusedData.authors
                bottomText.text = focusedData.title

                listOf(topText, bottomText).forEach {
                    it.animate().cancel()
                    it.alpha = 1f
                }
            }

    /**
     * Hides Pocket description text when the channel loses focus
     */
    private fun observePocketFocusLoss(): Disposable = pocketChannel.focusChangeObservable
            // Focus is lost every time a new tile is focused. To isolate events where focus
            // leaves the channel, we debounce to accept focus loss events not immediately
            // followed by focus gain events
            .debounce(10, TimeUnit.MILLISECONDS)
            .filter { (_, focusGained) -> !focusGained }
            .observeOn(AndroidSchedulers.mainThread()) // Debounce operates on computation
            .subscribe {
                val topText = pocketChannel.channelContainer.channelBelowText1
                val bottomText = pocketChannel.channelContainer.channelBelowText2

                listOf(topText, bottomText)
                        .forEach {
                            it.animate().cancel()
                            it.animate().alpha(0f).setDuration(150L).start()
                        }
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
        channelConfig = ChannelConfig.getPocketConfig()
    )

    val pinnedTileChannel = channelFactory.createChannel(
        parent = channelContainerView,
        id = R.id.pinned_tiles_channel,
        channelConfig = ChannelConfig.getPinnedTileConfig(channelContainerView.context)
    )
}
