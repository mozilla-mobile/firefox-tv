/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import org.mozilla.tv.firefox.pocket.PocketViewModel

/**
 * A collection of functions that generate test data for Pocket-related code.
 */
object PocketTestData {
    fun getVideoItem(id: Int) =
        PocketViewModel.FeedItem.Video(
            id = id,
            title = "Mozilla",
            url = "https://www.mozilla.org/en-US/",
            thumbnailURL = "https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png",
            popularitySortId = id,
            authors = "0:{'name':'Mozilla'}"
        )

    fun getVideoFeed(size: Int) = List(size) { getVideoItem(it) }
}
