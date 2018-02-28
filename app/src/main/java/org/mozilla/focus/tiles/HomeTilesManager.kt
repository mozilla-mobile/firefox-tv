/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tiles

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.support.annotation.UiThread
import org.json.JSONArray
import org.json.JSONObject

private const val PREF_HOME_TILES = "homeTiles"
private const val CUSTOM_SITES_LIST = "customSitesList"

private const val KEY_URL = "url"
private const val KEY_TITLE = "title"
private const val KEY_IMG = "img"
private const val KEY_ID = "id"

data class HomeTile(
        val url: String,
        val title: String,
        val imagePath: String?,
        /** Unique id used to identify specific home tiles, e.g. for deletion, etc. **/
        val id: String
) {

    fun toJSONObject() = JSONObject(mapOf(
                KEY_URL to url,
                KEY_TITLE to title,
                KEY_IMG to imagePath,
                KEY_ID to id
    ))

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): HomeTile {
            return HomeTile(jsonObject.getString(KEY_URL),
                    jsonObject.getString(KEY_TITLE),
                    jsonObject.optString(KEY_IMG),
                    jsonObject.getString(KEY_ID))
        }
    }
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

    private fun getCustomTilesCache(context: Context): LinkedHashMap<String, HomeTile> {
        val tilesJSONArray = getCustomSitesJSONArray(getHomeTilesPreferences(context))
        val lhm = LinkedHashMap<String, HomeTile>()
        for (i in 0 until tilesJSONArray.length()) {
            val tile = tilesJSONArray.getJSONObject(i)
            lhm.put(tile.getString(KEY_URL), HomeTile.fromJSONObject(tile))
        }
        return lhm
    }

    @UiThread
    fun isURLPinned(url: String) = customTilesCache.containsKey(url)

    @UiThread
    fun getCustomHomeTilesList() = customTilesCache.values.reversed()

    @UiThread
    fun pinSite(context: Context, url: String) {
        customTilesCache.put(url, HomeTile(url, url, "", url))
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
