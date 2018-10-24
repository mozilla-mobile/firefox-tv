/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations

/**
 * TODO
 *
 * AndroidViewModel() is a context aware ViewModel => Used for PinnedTileRepo
 */
class PinnedTileViewModel(application: Application, pinnedTileRepo: PinnedTileRepo) : AndroidViewModel(application) {

    private val _tilesList = MediatorLiveData<List<PinnedTile>>()
    val isEmpty: LiveData<Boolean> = Transformations.map(_tilesList) { input -> input.isEmpty() }

    init {
        // TODO: mediate data from PinnedTileRepo.tileList
        _tilesList.addSource(pinnedTileRepo.loadTilesCache(application)) { pinnedTileMap ->
            // FIXME: how to match type?
            val feedTileList: List<PinnedTile>? = pinnedTileMap?.values?.toMutableList()
            if ((feedTileList) != null) {
                _tilesList.value = feedTileList
            }
        }
    }

    // For UI injection
    fun getTileList(): LiveData<List<PinnedTile>> {
        return _tilesList
    }

    // TODO: PinnedTileRepo.removePinnedTile
    fun unpin() {}
}
