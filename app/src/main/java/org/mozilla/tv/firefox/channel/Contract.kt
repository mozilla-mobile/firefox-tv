///* This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
//
//package org.mozilla.tv.firefox.channel
//
//import android.content.Context
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//
//// This may or may not be used in the final code. Mostly here for planning purposes
//interface ChannelContract {
//
//    interface DefaultChannelAdapter {
//        fun setTiles(tileData: List<ChannelTile>)
//    }
//
//    interface DefaultChannelFactory {
//        // Returns a ViewGroup composed of a TextView and a RecyclerView (or ListRowView?)
//        // Attaches a DefaultChannelAdapter to the RV
//        fun createChannel(context: Context): Channel
//    }
//
//    interface Channel {
//        fun setDetails(title: CharSequence) // set title, etc
//        fun setContents(tileData: List<ChannelTile>) // notify changed, etc.
//
//        val containerView: ViewGroup
//        private val titleView: TextView
//        private val recyclerView: RecyclerView
//        private val adapter: ChannelAdapter
//    }
//
//    // Desired interface
//    /*
//        pinnedTileChannel = DefaultChannelFactory.createChannel(context)
//        channelContainer.addView(pinnedTileChannel.view)
//
//        pinnedTileVM.tiles.subscribe {
//            pinnedTileChannel.setDetails(it.details)
//            pinnedTileChannel.setContents(it.contents)
//        }
//
//     */
//
//
//
//}