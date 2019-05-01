/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channel

import android.widget.ImageView

/**
 * TODO
 */
data class ChannelTile(
        val url: String,
        val title: String,
        val setImage: (ImageView) -> Unit //TODO this is kinda hard to read in practice
)

data class ChannelDetails(
        // todo; change names, too similar
        val title: CharSequence,
        val tiles: List<ChannelTile>
)
