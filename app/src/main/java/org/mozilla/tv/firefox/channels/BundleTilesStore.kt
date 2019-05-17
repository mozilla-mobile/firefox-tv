/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.app.Application
import org.json.JSONArray

private const val BUNDLED_PINNED_TILES_DIR = "bundled"
private const val PINNED_TILES_JSON_PATH = "$BUNDLED_PINNED_TILES_DIR/bundled_tiles.json"

enum class BundleType {
    PINNED_TILES
}

/**
 * [BundleTilesStore] is responsible for fetching bundled tiles data from /assets/ with
 * [BundleType]
 */
class BundleTilesStore(private val applicationContext: Application) {
    fun getBundledTiles(type: BundleType): JSONArray {
        val jsonPath = when (type) {
            BundleType.PINNED_TILES -> PINNED_TILES_JSON_PATH
        }

        val tilesJSONString = applicationContext.assets.open(jsonPath).bufferedReader().use { it.readText() }
        return JSONArray(tilesJSONString)
    }
}
