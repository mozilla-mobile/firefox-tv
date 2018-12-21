/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import kotlinx.android.synthetic.main.browser_overlay.*
import kotlinx.android.synthetic.main.browser_overlay_top_nav.*
import kotlinx.android.synthetic.main.pocket_video_mega_tile.*
import kotlinx.coroutines.Job
import mozilla.components.browser.domains.DomainAutoCompleteProvider
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.forEachChild
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.isEffectivelyVisible
import org.mozilla.tv.firefox.ext.isVisible
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.updateLayoutParams
import org.mozilla.tv.firefox.pinnedtile.PinnedTileAdapter
import org.mozilla.tv.firefox.pinnedtile.PinnedTileViewModel
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.toolbar.ToolbarViewModel
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.widget.IgnoreFocusMovementMethod
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText
import kotlin.properties.Delegates

private const val NAVIGATION_BUTTON_ENABLED_ALPHA = 1.0f
private const val NAVIGATION_BUTTON_DISABLED_ALPHA = 0.3f

private const val SHOW_UNPIN_TOAST_COUNTER_PREF = "show_upin_toast_counter"
private const val MAX_UNPIN_TOAST_COUNT = 3

private const val COL_COUNT = 5
private val uiHandler = Handler(Looper.getMainLooper())

@Suppress("LargeClass") // TODO remove this. See https://github.com/mozilla-mobile/firefox-tv/issues/1187
class NavigationOverlayFragment : Fragment(), View.OnClickListener {
    companion object {
        const val FRAGMENT_TAG = "overlay"
    }

    sealed class Action {
        data class ShowTopToast(@StringRes val textId: Int) : Action()
        data class ShowBottomToast(@StringRes val textId: Int) : Action()
        data class SetOverlayVisible(val visible: Boolean) : Action()
    }

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    var uiLifecycleCancelJob: Job = Job()

    // We need this in order to show the unpin toast, at max, once per
    // instantiation of the BrowserNavigationOverlay
    var canShowUpinToast: Boolean = false

    // Setting the onTileLongClick function in the HomeTileAdapter is fragile
    // since we init the tiles in View.init and Android is inflating the view for us,
    // thus we need to use Delegates.observable to update onTileLongClick.
    var openHomeTileContextMenu: (() -> Unit) by Delegates.observable({}) { _, _, newValue ->
        with(tileContainer) {
            (adapter as PinnedTileAdapter).onTileLongClick = newValue
        }
    }

    var onNavigationEvent: ((
        event: NavigationEvent,
        value: String?,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?
    ) -> Unit)? = null

    private var currFocus: View? = null
        get() = activity?.currentFocus

    private var hasUserChangedURLSinceEditTextFocused = false

    private lateinit var serviceLocator: ServiceLocator
    private lateinit var toolbarViewModel: ToolbarViewModel
    private lateinit var pinnedTileViewModel: PinnedTileViewModel
    private lateinit var pocketViewModel: PocketViewModel

    private lateinit var tileAdapter: PinnedTileAdapter

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
        topNavContainer.forEachChild {
            it.nextFocusDownId = navUrlInput.id
            if (it.isFocusable) it.setOnClickListener(this)
        }

        setupUrlInput()
        initToolbar()
        initMegaTile()
        initPinnedTiles()

        navButtonSettings.setImageResource(R.drawable.ic_settings) // Must be set in code for SVG to work correctly.

        val tintDrawable: (Drawable?) -> Unit = { it?.setTint(ContextCompat.getColor(context!!, R.color.photonGrey10_a60p)) }
        navUrlInput.compoundDrawablesRelative.forEach(tintDrawable)

        updateFocusableViews()
    }

    private fun setupUrlInput() = with(navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput == URLs.APP_URL_HOME) {
                // If the input points to home, we short circuit and hide the keyboard, returning
                // the user to the home screen
                this.hideKeyboard()
                return@setOnCommitListener
            }

            if (userInput.isNotEmpty()) {
                val cachedAutocompleteResult = lastAutocompleteResult // setText clears the reference so we cache it here.
                setText(cachedAutocompleteResult.text)
                onNavigationEvent?.invoke(NavigationEvent.LOAD_URL, userInput, cachedAutocompleteResult)
            }
        }
        this.movementMethod = IgnoreFocusMovementMethod()
        val autocompleteProvider = DomainAutoCompleteProvider().apply {
            initialize(
                context = context,
                useShippedDomains = true,
                useCustomDomains = false,
                loadDomainsFromDisk = true
            )
        }
        setOnFilterListener { searchText, view ->
            val result = autocompleteProvider.autocomplete(searchText)
            view?.onAutocomplete(InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.size))
        }

        setOnUserInputListener { hasUserChangedURLSinceEditTextFocused = true }
        setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) hasUserChangedURLSinceEditTextFocused = false }
    }

    private fun initToolbar() {
        fun updateOverlayButtonState(isEnabled: Boolean, overlayButton: ImageButton) {
            overlayButton.isEnabled = isEnabled
            overlayButton.isFocusable = isEnabled
            overlayButton.alpha =
                    if (isEnabled) NAVIGATION_BUTTON_ENABLED_ALPHA else NAVIGATION_BUTTON_DISABLED_ALPHA
        }

        toolbarViewModel.state.observe(this, Observer {
            if (it == null) return@Observer
            val focusedView = currFocus
            updateOverlayButtonState(it.backEnabled, navButtonBack)
            updateOverlayButtonState(it.forwardEnabled, navButtonForward)
            updateOverlayButtonState(it.pinEnabled, pinButton)
            updateOverlayButtonState(it.refreshEnabled, navButtonReload)
            updateOverlayButtonState(it.desktopModeEnabled, desktopModeButton)

            updateFocusableViews(focusedView)

            pinButton.isChecked = it.pinChecked
            desktopModeButton.isChecked = it.desktopModeChecked
            turboButton.isChecked = it.turboChecked

            if (!hasUserChangedURLSinceEditTextFocused) {
                // The url can get updated in the background, e.g. if a loading page is redirected. We
                // don't want a url update to interrupt the user typing so we don't update the url from
                // the background if the user has already updated the url themselves.
                //
                // We revert this state when the view is unfocused: it ensures the URL is usually accurate
                // (for security reasons) and it's simple compared to other options which keep more state.
                //
                // One problem this solution has is that if the URL is updated in the background rapidly,
                // sometimes key events will be dropped, but I don't think there's much we can do about this:
                // we can't determine if the keyboard is up or not and focus isn't a good indicator because
                // we can focus the EditText without opening the soft keyboard and the user won't even know
                // these are inaccurate!
                navUrlInput.setText(it.urlBarText)
            }
        })

        toolbarViewModel.events.observe(this, Observer {
            it?.consume {
                when (it) {
                    is Action.ShowTopToast -> ViewUtils.showCenteredTopToast(context, it.textId)
                    is Action.ShowBottomToast -> ViewUtils.showCenteredBottomToast(context, it.textId)
                    is Action.SetOverlayVisible -> Unit
                }.forceExhaustive
                true
            }
        })
    }

    /**
     * Used to show an error screen on the Pocket megatile when Pocket does not return any videos.
     */
    fun showMegaTileError() {
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
        pocketVideoMegaTileView.setOnClickListener(this)

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
        canShowUpinToast = true

        // TODO: pass in VM live data instead of "homeTiles"
        tileAdapter = org.mozilla.tv.firefox.pinnedtile.PinnedTileAdapter(uiLifecycleCancelJob, loadUrl = { urlStr ->
            if (urlStr.isNotEmpty()) {
                onNavigationEvent?.invoke(org.mozilla.tv.firefox.navigationoverlay.NavigationEvent.LOAD_TILE, urlStr, null)
            }
        }, onTileLongClick = openHomeTileContextMenu, onTileFocused = {
            val prefInt = android.preference.PreferenceManager.getDefaultSharedPreferences(context).getInt(
                    org.mozilla.tv.firefox.navigationoverlay.SHOW_UNPIN_TOAST_COUNTER_PREF, 0)
            if (prefInt < org.mozilla.tv.firefox.navigationoverlay.MAX_UNPIN_TOAST_COUNT && canShowUpinToast) {
                android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putInt(org.mozilla.tv.firefox.navigationoverlay.SHOW_UNPIN_TOAST_COUNTER_PREF, prefInt + 1)
                        .apply()

                val contextReference = java.lang.ref.WeakReference(context)
                val showToast = showToast@{
                    val context = contextReference.get() ?: return@showToast
                    android.widget.Toast.makeText(context, org.mozilla.tv.firefox.R.string.homescreen_unpin_tutorial_toast,
                            android.widget.Toast.LENGTH_LONG).show()
                }
                if (context.isVoiceViewEnabled()) org.mozilla.tv.firefox.navigationoverlay.uiHandler.postDelayed(showToast, 1500)
                else showToast.invoke()

                canShowUpinToast = false
            }
        })

        pinnedTileViewModel.getTileList().observe(this@NavigationOverlayFragment, Observer {
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
        val tileBottomMargin = resources.getDimensionPixelSize(org.mozilla.tv.firefox.R.dimen.home_tile_margin_bottom) -
                resources.getDimensionPixelSize(org.mozilla.tv.firefox.R.dimen.home_tile_container_margin_bottom)
        updateLayoutParams {
            val marginLayoutParams = it as ViewGroup.MarginLayoutParams
            marginLayoutParams.bottomMargin = -tileBottomMargin
        }
    }

    override fun onClick(v: View?) {
        val event = NavigationEvent.fromViewClick(view?.id)
                ?: return

        when (event) {
            NavigationEvent.BACK -> toolbarViewModel.backButtonClicked()
            NavigationEvent.FORWARD -> toolbarViewModel.forwardButtonClicked()
            NavigationEvent.RELOAD -> toolbarViewModel.reloadButtonClicked()
            NavigationEvent.PIN_ACTION -> toolbarViewModel.pinButtonClicked()
            NavigationEvent.TURBO -> toolbarViewModel.turboButtonClicked()
            NavigationEvent.DESKTOP_MODE -> toolbarViewModel.desktopModeButtonClicked()
            else -> Unit // Nothing to do.
        }
        onNavigationEvent?.invoke(event, null, null)
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
