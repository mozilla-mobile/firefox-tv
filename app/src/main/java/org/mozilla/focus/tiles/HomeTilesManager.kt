/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tiles

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.support.annotation.UiThread
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.focus.utils.UrlUtils
import java.net.URL
import java.util.UUID

private const val PREF_HOME_TILES = "homeTiles"
private const val CUSTOM_SITES_LIST = "customSitesList"

private const val BUNDLED_HOME_TILES_DIR = "bundled/"
private const val HOME_TILES_JSON_PATH = BUNDLED_HOME_TILES_DIR + "bundled_tiles.json"
private const val HOME_TILES_JSON_KEY = "bundled_tiles"

class BundledTilesManager private constructor(context: Context) {
    companion object {
        private var thisInstance: BundledTilesManager? = null
        fun getInstance(context: Context): BundledTilesManager {
            if (thisInstance == null) {
                thisInstance = BundledTilesManager(context)
            }
            return thisInstance!!
        }
    }

    private var bundledTilesCache = loadBundledTilesCache(context)

    private fun loadBundledTilesCache(context: Context): LinkedHashMap<URL, BundledHomeTile> {
        val tilesJSONString = context.assets.open(HOME_TILES_JSON_PATH).bufferedReader().use { it.readText() }
        val tilesJSONArray = JSONObject(tilesJSONString).getJSONArray(HOME_TILES_JSON_KEY)
        val lhm = LinkedHashMap<URL, BundledHomeTile>()
        for (i in 0 until tilesJSONArray.length()) {
            val tile = BundledHomeTile.fromJSONObject(tilesJSONArray.getJSONObject(i))
            // TODO: Check for blacklisted sites and don't add them
            lhm.put(URL(tile.url), tile)
        }
        return lhm
    }

    @UiThread
    fun isURLPinned(urlString: String): Boolean {
        val testUrl = URL(urlString)
        for (u in bundledTilesCache.keys) {
            if (compareUrl(testUrl, u)) return true
        }
        return false
    }

    private fun compareUrl(url1: URL, url2: URL): Boolean {
        return url1.protocol == url2.protocol
                && UrlUtils.stripCommonSubdomains(url1.host) == UrlUtils.stripCommonSubdomains(url2.host)
                && url1.path == url2.path
                && url1.ref == url2.ref
    }

    @UiThread
    fun unpinSite(context: Context, url: String) {
        bundledTilesCache.remove(URL(url))
        // TODO: Add site to blacklist in Issue #443 to persist un-pinning of bundled sites
    }

    private fun getTileIdFromUrl(urlString: String): String? {
        val testUrl = URL(urlString)
        for (pair in bundledTilesCache) {
            if (compareUrl(testUrl, pair.key)) {
                return pair.value.id
            }
        }
        return null
    }

    fun loadImageFromPath(context: Context, path: String) = context.assets.open(BUNDLED_HOME_TILES_DIR + path).use {
            BitmapFactory.decodeStream(it)
        }

    @UiThread
    fun getBundledHomeTilesList() = bundledTilesCache.values
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
    private var customTilesCache = loadCustomTilesCache(context)

    private fun loadCustomTilesCache(context: Context): LinkedHashMap<String, CustomHomeTile> {
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
        // TODO: titles, screenshots/icons.
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
}

private fun getHomeTilesPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREF_HOME_TILES, MODE_PRIVATE)
}
