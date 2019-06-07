/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.content

import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ChannelTile
import org.mozilla.tv.firefox.channels.TileSource

fun ChannelContent.getNewsChannels(): List<ChannelTile> = listOf(
    ChannelTile(
        url = "https://us.cnn.com/videos",
        title = "CNN",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_cnn),
        tileSource = TileSource.TV_GUIDE,
        id = "cnn"
    ),
    ChannelTile(
        url = "https://video.foxnews.com/",
        title = "Fox News",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_fox_news),
        tileSource = TileSource.TV_GUIDE,
        id = "foxNews"
    ),
    ChannelTile(
        url = "https://www.washingtonpost.com/video/",
        title = "Washington Post",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_washington_post),
        tileSource = TileSource.TV_GUIDE,
        id = "washingtonPost"
    ),
    ChannelTile(
        url = "https://www.usatoday.com/media/latest/videos/news/",
        title = "USA Today",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_usa_today),
        tileSource = TileSource.TV_GUIDE,
        id = "usaToday"
    ),
    ChannelTile(
        url = "https://www.wsj.com/video/",
        title = "Wall Street Journal",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_wall_street_journal),
        tileSource = TileSource.TV_GUIDE,
        id = "wallStreetJournal"
    ),
    ChannelTile(
        url = "https://www.nbcnews.com/video",
        title = "NBC News",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_nbc_news),
        tileSource = TileSource.TV_GUIDE,
        id = "nbcNews"
    ),
    ChannelTile(
        url = "https://www.cbsnews.com/video/",
        title = "CBS News",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_cbs_news),
        tileSource = TileSource.TV_GUIDE,
        id = "cbsNews"
    ),
    ChannelTile(
        url = "https://www.huffpost.com/section/video",
        title = "Huffington Post",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_huffington_post),
        tileSource = TileSource.TV_GUIDE,
        id = "huffingtonPost"
    ),
    ChannelTile(
        url = "https://video.vice.com/",
        title = "Vice",
        subtitle = null,
        setImage = setImage(R.drawable.tile_news_vice),
        tileSource = TileSource.TV_GUIDE,
        id = "vice"
    )
)
