/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.content

import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ChannelTile
import org.mozilla.tv.firefox.channels.TileSource

fun ChannelContent.getSportsChannels(): List<ChannelTile> = listOf(
    ChannelTile(
        url = "https://www.nbcsports.com/video",
        title = "NBC Sports",
        subtitle = null,
        setImage = setImage(R.drawable.tile_sports_nbc_sports),
        tileSource = TileSource.TV_GUIDE,
        id = "nbcSports"
    ),
    ChannelTile(
        url = "https://www.formula1.com/en/video.html",
        title = "Formula 1",
        subtitle = null,
        setImage = setImage(R.drawable.cursor_active_bg), // TODO ask UX for image resource
        tileSource = TileSource.TV_GUIDE,
        id = "formula1"
    )
)
