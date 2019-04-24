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
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.navUrlInput
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.pocketVideoMegaTileView
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.settingsTileContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_orig.tileContainer
import kotlinx.android.synthetic.main.fragment_navigation_overlay_top_nav.exitButton
import kotlinx.android.synthetic.main.fragment_navigation_overlay_top_nav.navButtonForward
import kotlinx.android.synthetic.main.fragment_navigation_overlay_top_nav.navButtonReload
import kotlinx.android.synthetic.main.hint_bar.hintBarContainer
import kotlinx.android.synthetic.main.pocket_video_mega_tile.megaTileTryAgainButton
import kotlinx.android.synthetic.main.pocket_video_mega_tile.pocketErrorContainer
import kotlinx.android.synthetic.main.pocket_video_mega_tile.pocketMegaTileLoadError
import kotlinx.android.synthetic.main.pocket_video_mega_tile.pocketVideosContainer
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.architecture.FocusOnShowDelegate
import org.mozilla.tv.firefox.experiments.ExperimentConfig
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.isEffectivelyVisible
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.hint.HintBinder
import org.mozilla.tv.firefox.hint.HintViewModel
import org.mozilla.tv.firefox.hint.InactiveHintViewModel
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsChannelAdapter
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsScreen
import org.mozilla.tv.firefox.pinnedtile.PinnedTileAdapter
import org.mozilla.tv.firefox.pinnedtile.PinnedTileViewModel
import org.mozilla.tv.firefox.pocket.PocketVideoFragment
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText
import java.lang.ref.WeakReference

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private const val COL_COUNT = 5
private val uiHandler = Handler(Looper.getMainLooper())

enum class NavigationEvent {
    BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION, POCKET, DESKTOP_MODE, EXIT_FIREFOX,
    SETTINGS_DATA_COLLECTION, SETTINGS_CLEAR_COOKIES;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            R.id.pocketVideoMegaTileView -> POCKET
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

    private val openHomeTileContextMenu: () -> Unit = { activity?.openContextMenu(tileContainer) }

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
            NavigationEvent.POCKET -> {
                val (fragmentManager, activity) = Pair(fragmentManager, activity)
                if (fragmentManager != null && activity != null) {
                    serviceLocator.screenController.showPocketScreen(fragmentManager)
                }
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

    private var currFocus: View? = null
        get() = activity?.currentFocus

    private lateinit var serviceLocator: ServiceLocator
    private lateinit var navigationOverlayViewModel: NavigationOverlayViewModel
    private lateinit var toolbarViewModel: ToolbarViewModel
    private lateinit var pinnedTileViewModel: PinnedTileViewModel
    private lateinit var pocketViewModel: PocketViewModel
    private lateinit var hintViewModel: HintViewModel

    private lateinit var tileAdapter: PinnedTileAdapter

    // TODO: remove this when FocusRepo is in place #1395
    private var defaultFocusTag = NavigationOverlayFragment.FRAGMENT_TAG
    @Deprecated(message = "VM state should be used reactively, not imperatively. See #1395, which will fix this")
    private var lastPocketState: PocketViewModel.State? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceLocator = context!!.serviceLocator

        navigationOverlayViewModel = FirefoxViewModelProviders.of(this).get(NavigationOverlayViewModel::class.java)
        toolbarViewModel = FirefoxViewModelProviders.of(this).get(ToolbarViewModel::class.java)
        pinnedTileViewModel = FirefoxViewModelProviders.of(this).get(PinnedTileViewModel::class.java)
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

        initMegaTile()
        initPinnedTiles()
        initSettingsChannel() // When pulling everything into channels, add this to the channel RV

        exitButton.contentDescription = serviceLocator.experimentsProvider.getAAExitButtonExperiment(ExperimentConfig.AA_TEST)

        val tintDrawable: (Drawable?) -> Unit = { it?.setTint(ContextCompat.getColor(context!!, R.color.photonGrey10_a60p)) }
        navUrlInput.compoundDrawablesRelative.forEach(tintDrawable)

        // TODO: remove this when FocusRepo is in place #1395
        when (defaultFocusTag) {
            PocketVideoFragment.FRAGMENT_TAG -> {
                pocketVideoMegaTileView.requestFocus()
                defaultFocusTag = NavigationOverlayFragment.FRAGMENT_TAG
            }
            NavigationOverlayFragment.FRAGMENT_TAG -> navUrlInput.requestFocus()
        }

        registerForContextMenu(tileContainer)

        observeFocusState()
                .addTo(compositeDisposable)
    }

    override fun onStart() {
        super.onStart()
        observePocketState()
            .addTo(compositeDisposable)
        HintBinder.bindHintsToView(hintViewModel, hintBarContainer, animate = false)
                .forEach { compositeDisposable.add(it) }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        FocusOnShowDelegate().onHiddenChanged(this, hidden)
        super.onHiddenChanged(hidden)
    }

    private fun exitFirefox() {
        activity!!.moveTaskToBack(true)
    }

    private fun observeFocusState(): Disposable {
        return navigationOverlayViewModel.focusUpdate
            .subscribe { focusNode ->
                val focusedView = rootView.findViewById<View>(focusNode.viewId)
                focusNode.updateViewNodeTree(focusedView)
            }
    }

    private fun observePocketState(): Disposable {
        return pocketViewModel.state
            .subscribe { state ->
                @Suppress("DEPRECATION")
                lastPocketState = state
                when (state) {
                    is PocketViewModel.State.Error -> {
                        pocketVideoMegaTileView.visibility = View.VISIBLE
                        showMegaTileError()
                    }
                    is PocketViewModel.State.Feed -> {
                        pocketVideoMegaTileView.visibility = View.VISIBLE
                        pocketVideoMegaTileView.setContent(state.feed)
                        hideMegaTileError()
                    }
                    is PocketViewModel.State.NotDisplayed -> pocketVideoMegaTileView.visibility = View.GONE
                    null -> return@subscribe
                }.forceExhaustive
            }
    }

    /**
     * Used to show an error screen on the Pocket megatile when Pocket does not return any videos.
     */
    private fun showMegaTileError() {
        pocketVideosContainer.visibility = View.GONE
        pocketErrorContainer.visibility = View.VISIBLE

        pocketMegaTileLoadError.text = resources.getString(R.string.pocket_video_feed_failed_to_load,
                resources.getString(R.string.pocket_brand_name))
        megaTileTryAgainButton.contentDescription = resources.getString(R.string.pocket_video_feed_failed_to_load,
                resources.getString(R.string.pocket_brand_name)) + " " + resources.getString(R.string.pocket_video_feed_reload_button)

        megaTileTryAgainButton.setOnClickListener { _ ->
            pocketViewModel.update()
            initMegaTile()
            pocketVideoMegaTileView.requestFocus()
        }
    }

    private fun hideMegaTileError() {
        pocketVideosContainer.visibility = View.VISIBLE
        pocketErrorContainer.visibility = View.GONE
    }

    private fun initMegaTile() {
        pocketVideoMegaTileView.setOnClickListener { view ->
            val event = NavigationEvent.fromViewClick(view.id) ?: return@setOnClickListener
            onNavigationEvent.invoke(event, null, null)
        }
    }

    private fun initPinnedTiles() = with(tileContainer) {
        canShowUnpinToast = true

        // TODO: pass in VM live data instead of "homeTiles"
        tileAdapter = PinnedTileAdapter(uiLifecycleCancelJob, loadUrl = { urlStr ->
            if (urlStr.isNotEmpty()) {
                onNavigationEvent.invoke(NavigationEvent.LOAD_TILE, urlStr, null)
            }
        }, onTileLongClick = openHomeTileContextMenu, onTileFocused = {
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
                if (context.isVoiceViewEnabled()) uiHandler.postDelayed(showToast, 1500)
                else showToast.invoke()

                canShowUnpinToast = false
            }
        })

        pinnedTileViewModel.getTileList().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                tileAdapter.setTiles(it)
            }
        })

        adapter = tileAdapter

        layoutManager = HomeTileManager(context, COL_COUNT)

        setHasFixedSize(true)

        // We add bottomMargin to each tile in order to add spacing between them: this makes the
        // RecyclerView slightly larger than necessary and makes the default start screen scrollable
        // even though it doesn't need to be. To undo this, we add negative margins on the tile container.
        // I tried other solutions (ItemDecoration, dynamically changing margins) but this is more
        // complex because we need to relayout more than the changed view when adding/removing a row.
        val tileBottomMargin = resources.getDimensionPixelSize(R.dimen.home_tile_margin_bottom) -
                resources.getDimensionPixelSize(R.dimen.home_tile_container_margin_bottom)
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = -tileBottomMargin
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity?.menuInflater?.inflate(R.menu.menu_context_hometile, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val homeTileAdapter = tileContainer.adapter as PinnedTileAdapter
                val tileToRemove = homeTileAdapter.lastLongClickedTile ?: return false

                // This assumes that since we're deleting from a Home Tile object that we created
                // that the Uri is valid, so we do not do error handling here.
                // TODO: NavigationOverlayFragment->ViewModel->Repo
                pinnedTileViewModel.unpin(tileToRemove.url)
                checkIfTilesFocusNeedRefresh()
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileToRemove)
                return true
            }
            else -> return false
        }
    }

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

    /**
     * Focus may be lost if all pinned items are removed via onContextItemSelected()
     * FIXME: requires OverlayFragment (LifecycleOwner) -> OverlayVM -> FocusRepo
     */
    private fun checkIfTilesFocusNeedRefresh() {
        if (tileAdapter.itemCount == 0) {
            if (pocketVideosContainer.isVisible) {
                pocketVideoMegaTileView.requestFocus()
            } else {
                megaTileTryAgainButton.requestFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Since we start the async jobs in View.init and Android is inflating the view for us,
        // there's no good way to pass in the uiLifecycleJob. We could consider other solutions
        // but it'll add complexity that I don't think is probably worth it.
        uiLifecycleCancelJob.cancel()
    }

    inner class HomeTileManager(
        context: Context,
        colCount: Int
    ) : GridLayoutManager(context, colCount) {
        override fun onRequestChildFocus(parent: RecyclerView, state: RecyclerView.State, child: View, focused: View?): Boolean {
            var position = spanCount
            if (focused != null) {
                position = getPosition(focused)
            }

            // if position is less than spanCount, implies first row
            if (position < spanCount) {
                focused?.nextFocusUpId = when {
                    pocketVideosContainer.isEffectivelyVisible -> R.id.pocketVideoMegaTileView
                    megaTileTryAgainButton.isEffectivelyVisible -> R.id.megaTileTryAgainButton
                    else -> R.id.navUrlInput
                }
            }
            return super.onRequestChildFocus(parent, state, child, focused)
        }
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
