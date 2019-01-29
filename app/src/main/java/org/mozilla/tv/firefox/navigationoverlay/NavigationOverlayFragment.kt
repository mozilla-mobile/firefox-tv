/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import kotlinx.android.synthetic.main.browser_overlay.*
import kotlinx.android.synthetic.main.browser_overlay_top_nav.*
import kotlinx.android.synthetic.main.pocket_video_mega_tile.*
import kotlinx.coroutines.Job
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.experiments.ExperimentConfig
import org.mozilla.tv.firefox.ext.forEachChild
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.isEffectivelyVisible
import org.mozilla.tv.firefox.ext.isVisible
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.updateLayoutParams
import org.mozilla.tv.firefox.pinnedtile.PinnedTileAdapter
import org.mozilla.tv.firefox.pinnedtile.PinnedTileViewModel
import org.mozilla.tv.firefox.pocket.PocketVideoFragment
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.settings.SettingsFragment
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
    SETTINGS, BACK, FORWARD, RELOAD, LOAD_URL, LOAD_TILE, TURBO, PIN_ACTION, POCKET, DESKTOP_MODE, EXIT_FIREFOX;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.navButtonSettings -> SETTINGS
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            R.id.pocketVideoMegaTileView -> POCKET
            R.id.desktopModeButton -> DESKTOP_MODE
            R.id.exitButton -> EXIT_FIREFOX
            else -> null
        }
    }
}

@Suppress("LargeClass") // TODO remove this. See https://github.com/mozilla-mobile/firefox-tv/issues/1187
class NavigationOverlayFragment : Fragment() {
    companion object {
        const val FRAGMENT_TAG = "overlay"
    }

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    private val uiLifecycleCancelJob: Job = Job()

    // We need this in order to show the unpin toast, at max, once per
    // instantiation of the BrowserNavigationOverlay
    private var canShowUnpinToast: Boolean = false

    private val openHomeTileContextMenu: () -> Unit = { activity?.openContextMenu(tileContainer) }

    private val onNavigationEvent = { event: NavigationEvent, value: String?,
                                      autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? ->
        when (event) {
            NavigationEvent.SETTINGS -> serviceLocator.screenController.showSettingsScreen(fragmentManager!!)
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
            NavigationEvent.TURBO, NavigationEvent.PIN_ACTION, NavigationEvent.DESKTOP_MODE, NavigationEvent.BACK,
            NavigationEvent.FORWARD, NavigationEvent.RELOAD, NavigationEvent.EXIT_FIREFOX -> { /* not handled by this object */ }
        }
        Unit
    }

    private var currFocus: View? = null
        get() = activity?.currentFocus

    private lateinit var serviceLocator: ServiceLocator
    private lateinit var toolbarViewModel: ToolbarViewModel
    private lateinit var pinnedTileViewModel: PinnedTileViewModel
    private lateinit var pocketViewModel: PocketViewModel

    private lateinit var tileAdapter: PinnedTileAdapter

    // TODO: remove this when FocusRepo is in place #1395
    var defaultFocusTag = NavigationOverlayFragment.FRAGMENT_TAG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceLocator = context!!.serviceLocator

        val factory = serviceLocator.viewModelFactory
        toolbarViewModel = ViewModelProviders.of(this, factory).get(ToolbarViewModel::class.java)
        pinnedTileViewModel = ViewModelProviders.of(this, factory).get(PinnedTileViewModel::class.java)
        pocketViewModel = ViewModelProviders.of(this, factory).get(PocketViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.browser_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ToolbarUiController(
            toolbarViewModel,
            ::exitFirefox,
            { currFocus },
            ::updateFocusableViews,
            onNavigationEvent
        ).onCreateView(view, viewLifecycleOwner, fragmentManager!!)

        initMegaTile()
        initPinnedTiles()

        navButtonSettings.setImageResource(R.drawable.ic_settings) // Must be set in code for SVG to work correctly.
        exitButton.contentDescription = serviceLocator.experimentsProvider.getAAExitButtonExperiment(ExperimentConfig.AA_TEST)

        val tintDrawable: (Drawable?) -> Unit = { it?.setTint(ContextCompat.getColor(context!!, R.color.photonGrey10_a60p)) }
        navUrlInput.compoundDrawablesRelative.forEach(tintDrawable)

        overlayScrollView.scrollTo(0, 0)

        // TODO: remove this when FocusRepo is in place #1395
        when (defaultFocusTag) {
            SettingsFragment.FRAGMENT_TAG -> {
                navButtonSettings.requestFocus()
                defaultFocusTag = NavigationOverlayFragment.FRAGMENT_TAG
            }
            PocketVideoFragment.FRAGMENT_TAG -> {
                pocketVideoMegaTileView.requestFocus()
                defaultFocusTag = NavigationOverlayFragment.FRAGMENT_TAG
            }
            NavigationOverlayFragment.FRAGMENT_TAG -> navUrlInput.requestFocus()
        }

        registerForContextMenu(tileContainer)

        updateFocusableViews()
    }

    private fun exitFirefox() {
        activity!!.moveTaskToBack(true)
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
            updateFocusableViews()
            pocketVideoMegaTileView.requestFocus()
        }
        updateFocusableViews()
    }

    private fun hideMegaTileError() {
        pocketVideosContainer.visibility = View.VISIBLE
        pocketErrorContainer.visibility = View.GONE
        updateFocusableViews()
    }

    private fun initMegaTile() {
        pocketVideoMegaTileView.setOnClickListener { view ->
            val event = NavigationEvent.fromViewClick(view.id) ?: return@setOnClickListener
            onNavigationEvent.invoke(event, null, null)
        }

        pocketViewModel.state.observe(viewLifecycleOwner, Observer { state ->
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
                null -> return@Observer
            }.forceExhaustive
        })
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
                updateFocusableViews()
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
        updateLayoutParams {
            val marginLayoutParams = it as ViewGroup.MarginLayoutParams
            marginLayoutParams.bottomMargin = -tileBottomMargin
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

    fun updateFocusableViews(focusedView: View? = currFocus) { // TODO this will be replaced when FocusRepo is introduced
        val toolbarState = toolbarViewModel.state.value

        // Prevent the focus from looping to the bottom row when reaching the last
        // focusable element in the top row
        navButtonReload.nextFocusLeftId = when {
            toolbarState?.forwardEnabled == true -> R.id.navButtonForward
            toolbarState?.backEnabled == true -> R.id.navButtonBack
            else -> R.id.navButtonReload
        }
        navButtonForward.nextFocusLeftId = when {
            toolbarState?.backEnabled == true -> R.id.navButtonBack
            else -> R.id.navButtonForward
        }

        navUrlInput.nextFocusDownId = when {
            pocketViewModel.state.value is PocketViewModel.State.Feed -> R.id.pocketVideoMegaTileView
            pocketViewModel.state.value === PocketViewModel.State.Error -> R.id.megaTileTryAgainButton
            tileAdapter.itemCount == 0 -> R.id.navUrlInput
            else -> R.id.tileContainer
        }

        navUrlInput.nextFocusUpId = when {
            toolbarState?.backEnabled == true -> R.id.navButtonBack
            toolbarState?.forwardEnabled == true -> R.id.navButtonForward
            toolbarState?.refreshEnabled == true -> R.id.navButtonReload
            toolbarState?.pinEnabled == true -> R.id.pinButton
            else -> R.id.turboButton
        }

        pocketVideoMegaTileView.nextFocusDownId = when {
            tileAdapter.itemCount == 0 -> R.id.pocketVideoMegaTileView
            else -> R.id.tileContainer
        }

        // We may have lost focus when disabling the focused view above.
        // This looks more complex than is necessary, but the simpler implementation
        // led to problems. See the commit message for 45940fa
        val isFocusLost = focusedView != null && currFocus == null
        if (isFocusLost) {
            navUrlInput.requestFocus()
        }
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
        updateFocusableViews()
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
class BrowserNavigationOverlayScrollView(
    context: Context,
    attrs: AttributeSet
) : ScrollView(context, attrs) {

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
