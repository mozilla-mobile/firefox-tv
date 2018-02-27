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

object HomeTilesManager {
    fun pinSite(context: Context, url: String) {
        val sharedPreferences = getHomeTilesPreferences(context)
        val sitesJSONArray = getCustomSitesJSONArray(sharedPreferences)

        sitesJSONArray.put(makeSiteJSON(url))

        sharedPreferences.edit()
                .putString(CUSTOM_SITES_LIST, sitesJSONArray.toString())
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

    fun getCustomHomeTilesList(context: Context): List<HomeTile> {
        val sharedPreferences = context.getSharedPreferences(HOME_TILES_PREFS, MODE_PRIVATE)
        val sitesJSONArray = getCustomSitesJSONArray(sharedPreferences)
        val homeTiles = mutableListOf<HomeTile>()
        for (i in sitesJSONArray.length() - 1 downTo 0) {
            homeTiles.add(HomeTile(sitesJSONArray.getJSONObject(i)))
        }
        return homeTiles
    }
}
