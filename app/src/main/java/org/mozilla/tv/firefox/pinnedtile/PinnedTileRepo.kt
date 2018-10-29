/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import org.json.JSONArray
import java.util.UUID

private const val BUNDLED_SITES_ID_BLACKLIST = "blacklist"
private const val CUSTOM_SITES_LIST = "customSitesList"
private const val PREF_HOME_TILES = "homeTiles"
private const val BUNDLED_HOME_TILES_DIR = "bundled"
private const val HOME_TILES_JSON_PATH = "$BUNDLED_HOME_TILES_DIR/bundled_tiles.json"

/**
 * TODO
 * Some methods require applicationContext in order to access /assets/
 */
class PinnedTileRepo(private val applicationContext: Application) {
    private val _pinnedTiles = MutableLiveData<LinkedHashMap<String, PinnedTile>>()

    // Persist custom & bundled tiles size for telemetry
    var customTilesSize = 0
    var bundledTilesSize = 0

    private val _sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences(PREF_HOME_TILES, Context.MODE_PRIVATE)

    init {
        loadTilesCache()
    }

    private fun loadTilesCache() {
        val pinnedTiles = linkedMapOf<String, PinnedTile>().apply {
            putAll(loadBundledTilesCache())
            putAll(loadCustomTilesCache())
        }

        _pinnedTiles.value = pinnedTiles
    }

    fun getPinnedTiles(): LiveData<LinkedHashMap<String, PinnedTile>> {
        return _pinnedTiles
    }

    fun addPinnedTile(url: String, screenshot: Bitmap?) {
        val newPinnedTile = CustomPinnedTile(url, "custom", UUID.randomUUID()) // TODO: titles
        if (_pinnedTiles.value?.put(url, newPinnedTile) != null) return
        _pinnedTiles.value = _pinnedTiles.value
        persistCustomTiles()

        if (screenshot != null) {
            PinnedTileScreenshotStore.saveAsync(applicationContext, newPinnedTile.id, screenshot)
        }
        ++customTilesSize
    }

    /**
     * returns tile id of a Bundled tile or null if
     * it doesn't exist in the cache
     */
    @UiThread
    fun removePinnedTile(url: String): String? {
        val tileToRemove = _pinnedTiles.value?.remove(url) ?: return null
        _pinnedTiles.value = _pinnedTiles.value

        when (tileToRemove) {
            is BundledPinnedTile -> {
                val blackList = loadBlacklist()
                blackList.add(tileToRemove.id)
                saveBlackList(blackList)
                --bundledTilesSize
            }
            is CustomPinnedTile -> {
                persistCustomTiles()
                PinnedTileScreenshotStore.removeAsync(applicationContext, tileToRemove.id)
                --customTilesSize
            }
        }

        return tileToRemove.idToString()
    }

    @UiThread
    fun isUrlPinned(url: String): Boolean? = _pinnedTiles.value?.containsKey(url)

    private fun loadBlacklist(): MutableSet<String> {
        return _sharedPreferences.getStringSet(BUNDLED_SITES_ID_BLACKLIST, mutableSetOf())!!
    }

    private fun saveBlackList(blackList: MutableSet<String>) {
        _sharedPreferences.edit().putStringSet(BUNDLED_SITES_ID_BLACKLIST, blackList).apply()
    }

    private fun persistCustomTiles() {
        val tilesJSONArray = JSONArray()
        for (tile in _pinnedTiles.value!!.values) {
            if (tile is CustomPinnedTile) tilesJSONArray.put(tile.toJSONObject())
        }

        _sharedPreferences.edit().putString(CUSTOM_SITES_LIST, tilesJSONArray.toString()).apply()
    }

    private fun loadBundledTilesCache(): LinkedHashMap<String, BundledPinnedTile> {
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
        bundledTilesSize = lhm.size

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
        customTilesSize = lhm.size

        return lhm
    }

    @AnyThread
    fun loadImageFromPath(path: String) = applicationContext.assets.open(
            "$BUNDLED_HOME_TILES_DIR/$path").use {
        BitmapFactory.decodeStream(it)
    }
}
