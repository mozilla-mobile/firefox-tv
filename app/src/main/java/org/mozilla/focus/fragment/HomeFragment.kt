/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_home.*
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.utils.OnUrlEnteredListener

private const val COL_COUNT = 5

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {

    var onUrlEnteredListener = object : OnUrlEnteredListener {} // default impl does nothing.
    val urlAutoCompleteFilter = UrlAutoCompleteFilter()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        initTiles()
        initUrlInputView()
    }

    override fun onResume() {
        super.onResume()
        urlAutoCompleteFilter.load(context)
    }

    private fun initTiles() = with (tileContainer) {
        adapter = HomeTileAdapter(onUrlEnteredListener)
        layoutManager = GridLayoutManager(context, COL_COUNT)
        setHasFixedSize(true)
    }

    private fun initUrlInputView() = with (urlInputView) {
        setOnCommitListener {
            onUrlEnteredListener.onUrlEntered(text.toString())
        }
        setOnFilterListener { searchText, view -> urlAutoCompleteFilter.onFilter(searchText, view) }
    }

    companion object {
        const val FRAGMENT_TAG = "home"

        @JvmStatic
        fun create() = HomeFragment()
    }
}

private class HomeTileAdapter(val onUrlEnteredListener: OnUrlEnteredListener) :
        RecyclerView.Adapter<TileViewHolder>() {

    val tiles = listOf(
            HomeTile("https://youtube.com/tv", R.string.tile_youtube_tv,R.drawable.tile_youtube),
            HomeTile("https://www.google.com/search?tbm=vid", R.string.tile_google_video_search, R.drawable.tile_google),
            HomeTile("http://imdb.com", R.string.tile_imdb, R.drawable.tile_imdb),
            HomeTile("https://www.rottentomatoes.com", R.string.tile_rottentomatoes, R.drawable.tile_rotten_tomatoes),

            // order?
            HomeTile("http://metacritic.com", R.string.tile_metacritic, R.drawable.tile_metacritic),
            HomeTile("https://fandango.com", R.string.tile_fandango, R.drawable.tile_fandango),

            HomeTile("https://hollywoodreporter.com", R.string.tile_hollywood_reporter, R.drawable.tile_hollywood_reporter),
            HomeTile("https://flickr.com", R.string.tile_flickr, R.drawable.tile_flickr),
            HomeTile("https://instagram.com", R.string.tile_instagram, R.drawable.tile_instagram), // sign in required
            HomeTile("https://pinterest.com", R.string.tile_pinterest, R.drawable.tile_pinterest) // sign in required
    )

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = with (holder) {
        val item = tiles[position]
        titleView.setText(item.titleRes)
        iconView.setImageResource(item.imageRes)
        itemView.setOnClickListener { onUrlEnteredListener.onUrlEntered(item.url) }
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
        @StringRes val titleRes: Int,
        @DrawableRes val imageRes: Int
)
