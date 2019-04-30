/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import androidx.lifecycle.ViewModel
import io.reactivex.Observable

/**
 * Pinned Tile ViewModel.
 * This class provides pinned tiles data to [NavigationOverlayFragment]
 *
 * @property pinnedTileRepo allows 1:1 mapping to pinned tiles data
 * @constructor Transformation of the pinned tiles data being emitted by [PinnedTileRepo]
 */
class PinnedTileViewModel(private val pinnedTileRepo: PinnedTileRepo) : ViewModel() {

    val tileList: Observable<List<PinnedTile>> = pinnedTileRepo.pinnedTiles
            .map { it.values.toList() }

    val isEmpty = pinnedTileRepo.isEmpty

    fun unpin(url: String) {
        pinnedTileRepo.removePinnedTile(url)
    }
}
