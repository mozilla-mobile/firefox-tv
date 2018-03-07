/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import org.json.JSONArray
import org.mozilla.focus.ext.toUri
import org.mozilla.focus.utils.UrlUtils
import java.util.UUID

private const val PREF_HOME_TILES = "homeTiles"
private const val BUNDLED_SITES_ID_BLACKLIST = "blacklist"
private const val CUSTOM_SITES_LIST = "customSitesList"

private const val BUNDLED_HOME_TILES_DIR = "bundled"
private const val HOME_TILES_JSON_PATH = "$BUNDLED_HOME_TILES_DIR/bundled_tiles.json"

/**
 * Static accessor for bundled tiles, which are loaded from assets/bundled/bundled_tiles.json.
 *
 * The urls provided in the bundled tiles are expected to close matches (including on Uri.path,
 * scheme [http or https]) with the final site that is loaded (after any server redirects, etc).
 * That way we can clearly reflect "pinned" state of these sites on the homescreen by matching
 * by url.
 */
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

    /**
     * The number of tiles in this manager. This is more performant than
     * [#getBundledHomeTilesList].size, which returns a copy of the data.
     */
    val tileCount get() = bundledTilesCache.size

    private fun loadBundledTilesCache(context: Context): LinkedHashMap<Uri, BundledHomeTile> {
        val tilesJSONString = context.assets.open(HOME_TILES_JSON_PATH).bufferedReader().use { it.readText() }
        val tilesJSONArray = JSONArray(tilesJSONString)
        val lhm = LinkedHashMap<Uri, BundledHomeTile>(tilesJSONArray.length())
        val blacklist = loadBlacklist(context)
        for (i in 0 until tilesJSONArray.length()) {
            val tile = BundledHomeTile.fromJSONObject(tilesJSONArray.getJSONObject(i))
            if (!blacklist.contains(tile.id)) {
                lhm.put(tile.url.toUri()!!, tile)
            }
        }
        return lhm
    }

    @UiThread
    fun isURLPinned(uri: Uri): Boolean {
        return bundledTilesCache.keys.any { u -> compareUri(uri, u) }
    }

    /**
     * Make a best effort fuzzy compare (such as matching mobile versions of sites)
     */
    private fun compareUri(uri1: Uri, uri2: Uri): Boolean {
        return uri1.scheme == uri2.scheme &&
                UrlUtils.stripCommonSubdomains(uri1.authority) ==
                    UrlUtils.stripCommonSubdomains(uri2.authority) &&
                uri1.path == uri2.path &&
                uri1.fragment == uri2.fragment &&
                uri1.query == uri2.query
    }

    @UiThread
    fun unpinSite(context: Context, uri: Uri): Boolean {
        val blacklist = loadBlacklist(context)
        val newBlacklist = blacklist.toMutableSet()
        for (pair in bundledTilesCache) {
            if (compareUri(uri, pair.key)) {
                newBlacklist.add(pair.value.id)
                context.getSharedPreferences(PREF_HOME_TILES, MODE_PRIVATE).edit()
                        .putStringSet(BUNDLED_SITES_ID_BLACKLIST, newBlacklist)
                        .apply()
                bundledTilesCache.remove(pair.key)
                return true
            }
        }
        return false
    }

    @AnyThread
    fun loadImageFromPath(context: Context, path: String) = context.assets.open(
            "$BUNDLED_HOME_TILES_DIR/$path").use {
        BitmapFactory.decodeStream(it)
    }

    private fun loadBlacklist(context: Context): MutableSet<String> {
        return context.getSharedPreferences(PREF_HOME_TILES, MODE_PRIVATE).getStringSet(BUNDLED_SITES_ID_BLACKLIST, mutableSetOf())
    }

    @UiThread
    fun getBundledHomeTilesList() = bundledTilesCache.values.toMutableList()
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

    /**
     * The number of tiles in this manager. This is more performant than
     * [#getCustomHomeTilesList].size, which returns a copy of the data.
     */
    val tileCount get() = customTilesCache.size

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
    fun getCustomHomeTilesList() = customTilesCache.values.toList() // return a copy.

    @UiThread
    fun pinSite(context: Context, url: String, screenshot: Bitmap?) {
        // TODO: titles
        val uuid = UUID.randomUUID()
        customTilesCache[url] = CustomHomeTile(url, "custom", uuid)
        writeCacheToSharedPreferences(context)

        if (screenshot != null) {
            HomeTileScreenshotStore.saveAsync(context, uuid, screenshot)
        }
    }

    @UiThread
    fun unpinSite(context: Context, url: String): Boolean {
        val tile = customTilesCache.remove(url) ?: return false
        writeCacheToSharedPreferences(context)
        HomeTileScreenshotStore.removeAsync(context, tile.id)
        return true
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
