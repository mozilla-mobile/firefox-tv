/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.channel.ChannelDetails
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.focus.FocusRepo
import org.mozilla.tv.firefox.pinnedtile.PinnedTileImageUtilWrapper
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import org.mozilla.tv.firefox.utils.URLs

class NavigationOverlayViewModel(
        sessionRepo: SessionRepo,
        focusRepo: FocusRepo,
        private val pinnedTileRepo: PinnedTileRepo,
        private val imageUtilityWrapper: PinnedTileImageUtilWrapper,
        private val formattedDomainWrapper: FormattedDomainWrapper
) : ViewModel() {

    val focusUpdate = focusRepo.focusUpdate
    val focusRequests: Observable<Int> = focusRepo.defaultViewAfterScreenChange
            .filter { it.second == ActiveScreen.NAVIGATION_OVERLAY }
            .map { it.first.viewId }

    @Suppress("DEPRECATION")
    val viewIsSplit: LiveData<Boolean> = sessionRepo.legacyState.map {
        it.currentUrl != URLs.APP_URL_HOME
    }

    val pinnedTiles = pinnedTileRepo.pinnedTiles
            .observeOn(Schedulers.computation())
            .map { it.values.map { it.toChannelTile(imageUtilityWrapper, formattedDomainWrapper) } }
            .map { ChannelDetails(title = "Pinned Tiles", tiles = it) } // TODO extract string
            .observeOn(AndroidSchedulers.mainThread())

    fun unpin(url: String) {
        pinnedTileRepo.removePinnedTile(url)
    }
}
