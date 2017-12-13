/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R

import kotlinx.android.synthetic.main.fragment_home.*

private const val COL_COUNT = 5

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        initTiles()
    }

    // todo: resize tiles.
    private fun initTiles() = with (tileContainer) {
        adapter = HomeTileAdapter()
        layoutManager = GridLayoutManager(context, COL_COUNT)
        setHasFixedSize(true)
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
