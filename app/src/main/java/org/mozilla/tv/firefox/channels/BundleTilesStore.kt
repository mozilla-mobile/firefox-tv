/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Collections

private const val PREF_BUNDLE_TILES = "bundleTiles"

// PinnedTiles
private const val BUNDLED_PINNED_TILES_DIR = "bundled"
private const val PINNED_TILES_JSON_PATH = "$BUNDLED_PINNED_TILES_DIR/bundled_tiles.json"
private const val BUNDLED_PINNED_SITES_ID_BLACKLIST = "blacklist"

enum class BundleType {
    PINNED_TILES
}

/**
 * [BundleTilesStore] is responsible for fetching bundled tiles data from /assets/ with
 * [BundleType]
 */
class BundleTilesStore(private val applicationContext: Context) {

    private val _sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences(PREF_BUNDLE_TILES, Context.MODE_PRIVATE)

    private fun loadBlackList(type: BundleType): Set<String> {
        val sharedPrefKey = when (type) {
            BundleType.PINNED_TILES -> BUNDLED_PINNED_SITES_ID_BLACKLIST
        }

        return _sharedPreferences.getStringSet(sharedPrefKey, Collections.emptySet())!!
    }

    private fun saveBlackList(type: BundleType, blackList: Set<String>) {
        val sharedPrefKey = when (type) {
            BundleType.PINNED_TILES -> BUNDLED_PINNED_SITES_ID_BLACKLIST
        }

        _sharedPreferences.edit().putStringSet(sharedPrefKey, blackList).apply()
    }

    fun getBundledTiles(type: BundleType): JSONArray {
        val jsonPath = when (type) {
            BundleType.PINNED_TILES -> PINNED_TILES_JSON_PATH
        }

        val blacklist = loadBlackList(type)
        val tilesJSONString = applicationContext.assets.open(jsonPath).bufferedReader().use { it.readText() }
        val tilesJSONArray = JSONArray(tilesJSONString)

        for (i in 0 until tilesJSONArray.length()) {
            val jsonObject = tilesJSONArray.getJSONObject(i)
            if (!blacklist.contains(jsonObject.getString("id"))) {
                tilesJSONArray.remove(i)
            }
        }

        return tilesJSONArray
    }

    /**
     * Used to handle removing bundle tiles by adding to its [BundleType] blacklist in SharedPreferences
     */
    fun addBundleTileToBlackList(type: BundleType, id: String) {
        val blackList = loadBlackList(type).toMutableSet()
        blackList.add(id)
        saveBlackList(type, blackList)
    }
}
