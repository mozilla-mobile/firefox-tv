/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel

/**
 * TODO
 */
class PinnedTileViewModel : ViewModel() {

    private val _tilesList = MediatorLiveData<List<PinnedTile>>()

    val isEmpty: LiveData<Boolean> = Transformations.map(_tilesList) { input -> input.isEmpty() }

    init {
        // TODO mediate data from PinnedTileRepo.tileList
    }

    // For UI injection
    fun getTileList(): LiveData<List<PinnedTile>> {
        return _tilesList
    }

    // TODO: PinnedTileRepo.removeHomeTile
    fun unpin() {}
}
