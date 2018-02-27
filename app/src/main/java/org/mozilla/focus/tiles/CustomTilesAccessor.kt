/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tiles

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

const val HOME_TILES_PREFS = "hometilesPrefs"
const val CUSTOM_SITES_LIST = "customSitesList"

const val KEY_URL = "url"
const val KEY_TITLE = "title"
const val KEY_IMG = "img"
const val KEY_ID = "identifier"

data class HomeTile (
        val url: String,
        val title: String,
        val imagePath: String?,
        // unique id used to identify specific home tiles, e.g. for deletion, etc.
        val id: String
) {
    constructor(jsonObject: JSONObject): this(jsonObject.getString(KEY_URL),
            jsonObject.getString(KEY_TITLE),
            jsonObject.optString(KEY_IMG),
            jsonObject.getString(KEY_ID))
}

/**
 * Static accessor of custom home tiles, that is backed by SharedPreferences.
 *
 * New sites are appended to the end of the list.
 *
 * This keeps a cached version of the custom home tiles that have been pinned,
 * in order to be more performant when checking whether sites are pinned or not.
 * In order to keep the cache consistent, should only be called from the UIThread.
 */
object CustomTilesAccessor {
    private lateinit var customTilesCache: LinkedHashMap<String, JSONObject>

    // Cache pinned sites for perf beacues we need to check pinned state for every page load
    fun initWithCache(customTiles: LinkedHashMap<String, JSONObject>) {
        customTilesCache = customTiles
    }

    fun getCustomTilesCache(context: Context): LinkedHashMap<String, JSONObject> {
        val tilesJSONArray = getCustomSitesJSONArray(getHomeTilesPreferences(context))
        val lhm = LinkedHashMap<String, JSONObject>()
        for (i in 0 until tilesJSONArray.length()) {
            val tile = tilesJSONArray.getJSONObject(i)
            lhm.put(tile.getString(KEY_URL), tile)
        }
        return lhm
    }

    fun isURLPinned(url: String): Boolean {
        return customTilesCache.containsKey(url)
    }

    fun getCustomHomeTilesList() = customTilesCache.values.map { HomeTile(it) }.reversed()

    fun pinSite(context: Context, url: String) {
        customTilesCache.put(url, makeSiteJSON(url))

        // Write cache to SharedPreferences
        val tilesJSONArray = JSONArray()
        for (tile in customTilesCache.values) {
            tilesJSONArray.put(tile)
        }

        getHomeTilesPreferences(context).edit()
                .putString(CUSTOM_SITES_LIST, tilesJSONArray.toString())
                .apply()
    }

    private fun getCustomSitesJSONArray(sharedPreferences: SharedPreferences): JSONArray {
        val sitesListString = sharedPreferences.getString(CUSTOM_SITES_LIST, null)
        return if (sitesListString != null) JSONArray(sitesListString) else JSONArray()
    }

    private fun makeSiteJSON(url: String): JSONObject {
        val siteJSON = JSONObject()
        siteJSON.put(KEY_URL, url)
        siteJSON.put(KEY_TITLE, url)
        siteJSON.put(KEY_IMG, null)
        siteJSON.put(KEY_ID, url)
        return siteJSON
    }

    private fun getHomeTilesPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(HOME_TILES_PREFS, MODE_PRIVATE)
    }
}
