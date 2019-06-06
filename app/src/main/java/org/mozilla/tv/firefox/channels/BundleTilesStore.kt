/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections

private const val PREF_BUNDLE_TILES = "bundleTiles"

// PinnedTiles

private const val BUNDLED_PINNED_SITES_ID_BLACKLIST = "blacklist"


/**
 * [BundleTilesStore] is responsible for fetching bundled tiles data from /assets/ with
 * [BundleType]
 */
class BundleTilesStore(private val applicationContext: Context) {

    private val _sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences(PREF_BUNDLE_TILES, Context.MODE_PRIVATE)
}
