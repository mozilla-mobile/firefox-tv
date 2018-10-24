/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.arch.lifecycle.MutableLiveData
import android.net.Uri

/**
 * TODO
 */
class PinnedTileRepo {
    val pinnedTile = MutableLiveData<LinkedHashMap<Uri, PinnedTile>>()

    fun loadTilesCache(): List<PinnedTile> {

        return listOf() // TODO
    }

    fun loadImageFromPath() {} // TODO

    fun loadBlacklist() {} // TODO

    fun removeHomeTile() {} // TODO

    fun isUrlPinned() {} // TODO
}
