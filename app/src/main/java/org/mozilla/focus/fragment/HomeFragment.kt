/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.ext.forceExhaustive
import org.mozilla.focus.home.HomeTileScreenshotStore
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.tiles.BundledHomeTile
import org.mozilla.focus.tiles.BundledTilesManager
import org.mozilla.focus.tiles.CustomHomeTile
import org.mozilla.focus.tiles.CustomTilesManager
import org.mozilla.focus.tiles.HomeTile
import org.mozilla.focus.utils.OnUrlEnteredListener

private const val COL_COUNT = 5
private const val SETTINGS_ICON_IDLE_ALPHA = 0.4f
private const val SETTINGS_ICON_ACTIVE_ALPHA = 1.0f

/**
 * Duration of animation to show custom tile. If the duration is too short, the tile will just
 * pop-in. I speculate this happens because the amount of time it takes to downsample the bitmap
 * is longer than the animation duration.
 */
private const val CUSTOM_TILE_ICON_TO_SHOW_MILLIS = 200L
private val CUSTOM_TILE_ICON_INTERPOLATOR = DecelerateInterpolator()

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {

    lateinit var urlBar: LinearLayout
    var onUrlEnteredListener = object : OnUrlEnteredListener {} // default impl does nothing.
    var onSettingsPressed: (() -> Unit)? = null
    val urlAutoCompleteFilter = UrlAutoCompleteFilter()

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    private lateinit var uiLifecycleCancelJob: Job

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
        urlBar = rootView.findViewById(R.id.homeUrlBar)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        uiLifecycleCancelJob = Job()

        initTiles()
        initUrlInputView()

        settingsButton.alpha = SETTINGS_ICON_IDLE_ALPHA
        settingsButton.setImageResource(R.drawable.ic_settings)
        settingsButton.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) SETTINGS_ICON_ACTIVE_ALPHA else SETTINGS_ICON_IDLE_ALPHA
        }

        settingsButton.setOnClickListener { v ->
            onSettingsPressed?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiLifecycleCancelJob.cancel(CancellationException("Parent lifecycle has ended"))
    }

    override fun onResume() {
        super.onResume()
        urlAutoCompleteFilter.load(context)
    }

    override fun onAttachFragment(childFragment: Fragment?) {
        super.onAttachFragment(childFragment)
        urlBar.requestFocus()
    }

    private fun initTiles() = with (tileContainer) {
        val homeTiles = mutableListOf<HomeTile>().apply {
            addAll(CustomTilesManager.getInstance(context).getCustomHomeTilesList())
            addAll(BundledTilesManager.getInstance(context).getBundledHomeTilesList())
        }

        adapter = HomeTileAdapter(uiLifecycleCancelJob, homeTiles, onUrlEnteredListener)
        layoutManager = GridLayoutManager(context, COL_COUNT)
        setHasFixedSize(true)
    }

    private fun initUrlInputView() = with (urlInputView) {
        setOnCommitListener {
            onUrlEnteredListener.onTextInputUrlEntered(text.toString(), urlInputView.lastAutocompleteResult, UrlTextInputLocation.HOME)
        }
        setOnFilterListener { searchText, view -> urlAutoCompleteFilter.onFilter(searchText, view) }
    }

    companion object {
        const val FRAGMENT_TAG = "home"

        @JvmStatic
        fun create() = HomeFragment()
    }
}

private class HomeTileAdapter(
        private val uiLifecycleCancelJob: Job,
        private val tiles: List<HomeTile>,
        private val onUrlEnteredListener: OnUrlEnteredListener
) : RecyclerView.Adapter<TileViewHolder>() {

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = with (holder) {
        val item = tiles[position]
        when (item) {
            is BundledHomeTile -> onBindBundledHomeTile(holder, item)
            is CustomHomeTile -> onBindCustomHomeTile(uiLifecycleCancelJob, holder, item)
        }.forceExhaustive

        titleView.setText(item.title)

        itemView.setOnClickListener {
            onUrlEnteredListener.onNonTextInputUrlEntered(item.url)
            TelemetryWrapper.homeTileClickEvent()
        }

        itemView.setOnFocusChangeListener { v, hasFocus ->
            val backgroundResource: Int
            val textColor: Int
            if (hasFocus) {
                backgroundResource = R.drawable.home_tile_title_focused_background
                textColor = Color.WHITE
            } else {
                backgroundResource = 0
                textColor = Color.BLACK
            }
            titleView.setBackgroundResource(backgroundResource)
            titleView.setTextColor(textColor)
        }
    }

    override fun getItemCount() = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.home_tile, parent, false)
    )
}

private fun onBindBundledHomeTile(holder: TileViewHolder, tile: BundledHomeTile) = with (holder) {
    val bitmap = BundledTilesManager.getInstance(itemView.context).loadImageFromPath(itemView.context, tile.imagePath)
    iconView.setImageBitmap(bitmap)
}

private fun onBindCustomHomeTile(uiLifecycleCancelJob: Job, holder: TileViewHolder, item: CustomHomeTile) = with (holder) {
    launch(uiLifecycleCancelJob + CommonPool) {
        val screenshot = HomeTileScreenshotStore.read(itemView.context, item.id) // TODO: if null, provide placeholder.
        launch(UI) {
            // Animate to avoid pop-in due to thread hand-offs. TODO: animation is janky.
            ObjectAnimator.ofInt(iconView, "imageAlpha", 0, 255).apply {
                interpolator = CUSTOM_TILE_ICON_INTERPOLATOR
                duration = CUSTOM_TILE_ICON_TO_SHOW_MILLIS
            }.start()
            iconView.setImageBitmap(screenshot)
        }
    }
}

private class TileViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.findViewById<ImageView>(R.id.tile_icon)
    val titleView = itemView.findViewById<TextView>(R.id.tile_title)
}
