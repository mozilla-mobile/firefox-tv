/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel

/**
 * Pinned Tile ViewModel.
 * This class provides pinned tiles data to [NavigationOverlayFragment]
 *
 * @property pinnedTileRepo allows 1:1 mapping to pinned tiles data
 * @constructor Transformation of the pinned tiles data being emitted by [PinnedTileRepo]
 */
class PinnedTileViewModel(private val pinnedTileRepo: PinnedTileRepo) : ViewModel() {

    private val _tilesList = MediatorLiveData<List<PinnedTile>>()
    val isEmpty = pinnedTileRepo.isEmpty

    init {
        @Suppress("DEPRECATION")
        _tilesList.addSource(pinnedTileRepo.legacyPinnedTiles) { pinnedTileMap ->
            val feedTileList: List<PinnedTile>? = pinnedTileMap?.values?.toList()
            if ((feedTileList) != null) {
                _tilesList.value = feedTileList
            }
        }
    }

    // For UI injection
    fun getTileList(): LiveData<List<PinnedTile>> {
        return _tilesList
    }

    fun unpin(url: String) {
        pinnedTileRepo.removePinnedTile(url)
    }
}
