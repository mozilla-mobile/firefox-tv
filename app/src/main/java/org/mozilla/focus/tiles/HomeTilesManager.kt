/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tiles

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.support.annotation.UiThread
import org.json.JSONArray
import java.util.UUID

private const val PREF_HOME_TILES = "homeTiles"
private const val CUSTOM_SITES_LIST = "customSitesList"

/**
 * Static accessor of custom home tiles, that is backed by SharedPreferences.
 *
 * New sites are appended to the end of the list.
 *
 * This keeps a cached version of the custom home tiles that have been pinned,
 * in order to be more performant when checking whether sites are pinned or not.
 * In order to keep the cache consistent, should only be called from the UIThread.
 */
class CustomTilesManager private constructor(context: Context) {
    companion object {
        private var thisInstance: CustomTilesManager? = null
        fun getInstance(context: Context): CustomTilesManager {
            if (thisInstance == null) {
                thisInstance = CustomTilesManager(context)
            }
            return thisInstance!!
        }
    }

    // Cache pinned sites for perf beacues we need to check pinned state for every page load
    private var customTilesCache = getCustomTilesCache(context)

    private fun getCustomTilesCache(context: Context): LinkedHashMap<String, CustomHomeTile> {
        val tilesJSONArray = getCustomSitesJSONArray(getHomeTilesPreferences(context))
        val lhm = LinkedHashMap<String, CustomHomeTile>()
        for (i in 0 until tilesJSONArray.length()) {
            val tileJSON = tilesJSONArray.getJSONObject(i)
            val tile = CustomHomeTile.fromJSONObject(tileJSON)
            lhm.put(tile.url, tile)
        }
        return lhm
    }

    @UiThread
    fun isURLPinned(url: String) = customTilesCache.containsKey(url)

    @UiThread
    fun getCustomHomeTilesList() = customTilesCache.values.reversed()

    @UiThread
    fun pinSite(context: Context, url: String) {
        customTilesCache.put(url, CustomHomeTile(url, "custom", UUID.randomUUID()))
        writeCacheToSharedPreferences(context)
    }

    @UiThread
    fun unpinSite(context: Context, url: String) {
        customTilesCache.remove(url)
        writeCacheToSharedPreferences(context)
    }

    private fun writeCacheToSharedPreferences(context: Context) {
        val tilesJSONArray = JSONArray()
        for (tile in customTilesCache.values) {
            tilesJSONArray.put(tile.toJSONObject())
        }

        getHomeTilesPreferences(context).edit()
                .putString(CUSTOM_SITES_LIST, tilesJSONArray.toString())
                .apply()
    }

    private fun getCustomSitesJSONArray(sharedPreferences: SharedPreferences): JSONArray {
        val sitesListString = sharedPreferences.getString(CUSTOM_SITES_LIST, "[]")
        return JSONArray(sitesListString)
    }

    private fun getHomeTilesPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_HOME_TILES, MODE_PRIVATE)
    }
}
