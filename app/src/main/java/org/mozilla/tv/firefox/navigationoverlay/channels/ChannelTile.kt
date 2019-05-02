/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Backing data for a [RecyclerView] item in a channel
 */
data class ChannelTile(
    val url: String,
    val title: String,
    val setImage: (ImageView) -> Unit
)

/**
 * Backing data for a channel as a whole
 */
data class ChannelDetails(
    val title: CharSequence,
    val tileList: List<ChannelTile>
)
