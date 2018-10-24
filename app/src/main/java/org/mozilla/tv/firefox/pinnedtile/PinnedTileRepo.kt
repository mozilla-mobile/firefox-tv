/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.support.annotation.AnyThread
import org.json.JSONArray

private const val BUNDLED_SITES_ID_BLACKLIST = "blacklist"
private const val CUSTOM_SITES_LIST = "customSitesList"

private const val BUNDLED_HOME_TILES_DIR = "bundled"
private const val HOME_TILES_JSON_PATH = "$BUNDLED_HOME_TILES_DIR/bundled_tiles.json"

/**
 * TODO
 * Some methods require applicationContext in order to access /assets/
 */
class PinnedTileRepo(sharedPreferences: SharedPreferences) {

    private val _pinnedTiles = MutableLiveData<LinkedHashMap<String, PinnedTile>>() // FIXME: URI? String?
    val tileCount get() = _pinnedTiles.value?.size

    // TODO:  applicationContext.getSharedPreferences(PREF_HOME_TILES, Context.MODE_PRIVATE)
    private var _sharedPreferences: SharedPreferences = sharedPreferences

    fun loadTilesCache(applicationContext: Context): MutableLiveData<LinkedHashMap<String, PinnedTile>> {
        val pinnedTiles = linkedMapOf<String, PinnedTile>().apply {
            putAll(loadBundledTilesCache(applicationContext))
            putAll(loadCustomTilesCache())
        }

        _pinnedTiles.value = pinnedTiles

        return _pinnedTiles
    }

    fun addPinnedTile() {} // TODO

    fun removePinnedTile() {} // TODO

    fun isUrlPinned(url: String): Boolean? = _pinnedTiles.value?.containsKey(url)

    private fun loadBlacklist(): MutableSet<String> { // TODO
        return _sharedPreferences.getStringSet(BUNDLED_SITES_ID_BLACKLIST, mutableSetOf())!!
    }

    private fun loadBundledTilesCache(applicationContext: Context): LinkedHashMap<String, BundledPinnedTile> {
        val tilesJSONString = applicationContext.assets.open(HOME_TILES_JSON_PATH).bufferedReader().use { it.readText() }
        val tilesJSONArray = JSONArray(tilesJSONString)
        val lhm = LinkedHashMap<String, BundledPinnedTile>(tilesJSONArray.length())
        val blacklist = loadBlacklist()
        for (i in 0 until tilesJSONArray.length()) {
            val tile = BundledPinnedTile.fromJSONObject(tilesJSONArray.getJSONObject(i))
            if (!blacklist.contains(tile.id)) {
                lhm.put(tile.url, tile)
            }
        }
        return lhm
    }

    private fun loadCustomTilesCache(): LinkedHashMap<String, CustomPinnedTile> {
        val tilesJSONArray = JSONArray(_sharedPreferences.getString(CUSTOM_SITES_LIST, "[]"))
        val lhm = LinkedHashMap<String, CustomPinnedTile>()
        for (i in 0 until tilesJSONArray.length()) {
            val tileJSON = tilesJSONArray.getJSONObject(i)
            val tile = CustomPinnedTile.fromJSONObject(tileJSON)
            lhm.put(tile.url, tile)
        }
        return lhm
    }

    @AnyThread
    fun loadImageFromPath(applicationContext: Context, path: String) = applicationContext.assets.open(
            "$BUNDLED_HOME_TILES_DIR/$path").use {
        BitmapFactory.decodeStream(it)
    }
}
