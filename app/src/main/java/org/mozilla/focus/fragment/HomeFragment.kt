/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_home.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.utils.ViewUtils

private const val COL_COUNT = 5

interface OnUrlEnteredListener {
    fun onUrlEntered(urlStr: String, searchTerms: String?) {}
}

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {

    var onUrlEnteredListener = object : OnUrlEnteredListener {} // default impl does nothing.

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        initTiles()
        initUrlInputView()
    }

    // todo: resize tiles.
    private fun initTiles() = with (tileContainer) {
        adapter = HomeTileAdapter()
        layoutManager = GridLayoutManager(context, COL_COUNT)
        setHasFixedSize(true)
    }

    private fun initUrlInputView() = with (urlInputView) {
        setOnTextChangeListener { originalText, autocompleteText ->  } // todo
        setOnCommitListener {
            val input = urlInputView.text.toString()

            if (input.trim { it <= ' ' }.isNotEmpty()) {
                ViewUtils.hideKeyboard(urlInputView)

                val isUrl = UrlUtils.isUrl(input)
                val url = if (isUrl) UrlUtils.normalize(input) else UrlUtils.createSearchUrl(context, input)
                val searchTerms = if (isUrl) null else input.trim { it <= ' ' }

                // todo: should input conversion happen in MainActivity?
                onUrlEnteredListener.onUrlEntered(url, searchTerms)
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "home"

        @JvmStatic
        fun create() = HomeFragment()
    }
}

private const val TILE_COUNT = 10

private class HomeTileAdapter : RecyclerView.Adapter<TileViewHolder>() {

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
    }

    override fun getItemCount() = TILE_COUNT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.home_tile, parent, false)
    )
}

private class TileViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView)
