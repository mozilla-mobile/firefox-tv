/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_home.*
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener

private const val COL_COUNT = 5

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {
    lateinit var urlBar: LinearLayout
    var onUrlEnteredListener = object : OnUrlEnteredListener {} // default impl does nothing.
    var onSettingsPressed: (() -> Unit)? = null
    val urlAutoCompleteFilter = UrlAutoCompleteFilter()
    val HOME_TILES_DIR = "defaults/"
    val HOME_TILES_JSON_PATH = HOME_TILES_DIR + "default_tiles.json"
    val HOME_TILES_JSON_KEY = "default_tiles"
    val SETTINGS_ICON_IDLE_ALPHA = 0.4f
    val SETTINGS_ICON_ACTIVE_ALPHA = 1.0f

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
        urlBar = rootView.findViewById(R.id.homeUrlBar)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
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

    override fun onResume() {
        super.onResume()
        urlAutoCompleteFilter.load(context)
    }

    override fun onAttachFragment(childFragment: Fragment?) {
        super.onAttachFragment(childFragment)
        urlBar.requestFocus()
    }

    private fun initTiles() = with (tileContainer) {
        val homeTiles = mutableListOf<HomeTile>()
        val inputAsString = context.assets.open(HOME_TILES_JSON_PATH).bufferedReader().use { it.readText() }
        val jsonArray = JSONObject(inputAsString).getJSONArray(HOME_TILES_JSON_KEY)
        for (i in 0..(jsonArray.length() - 1)) {
            val jsonObject = jsonArray.getJSONObject(i)
            val url = jsonObject.getString("url")
            val title = jsonObject.getString("title")
            val imgPath = HOME_TILES_DIR + jsonObject.getString("img")
            val id = jsonObject.getString("identifier")
            homeTiles.add(HomeTile(url, title, imgPath, id))
        }
        adapter = HomeTileAdapter(onUrlEnteredListener, homeTiles)
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

private class HomeTileAdapter(val onUrlEnteredListener: OnUrlEnteredListener, homeTiles: MutableList<HomeTile>) :
        RecyclerView.Adapter<TileViewHolder>() {
    val tiles = homeTiles

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = with (holder) {
        val item = tiles[position]
        titleView.setText(item.title)
        val bmImg = itemView.context.assets.open(item.imagePath).use { BitmapFactory.decodeStream(it) }
        iconView.setImageBitmap(bmImg)
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

private class TileViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.findViewById<ImageView>(R.id.tile_icon)
    val titleView = itemView.findViewById<TextView>(R.id.tile_title)
}

private data class HomeTile (
        val url: String,
        val title: String,
        val imagePath: String,
        // unique id used to identify specific home tiles, e.g. for deletion, etc.
        val id: String
)
