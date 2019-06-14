/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.channels.ChannelDetails
import org.mozilla.tv.firefox.channels.ChannelRepo
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.focus.FocusRepo
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class ChannelTitles(
    val pinned: String,
    val newsAndPolitics: String,
    val sports: String,
    val music: String,
    val food: String
)

class NavigationOverlayViewModel(
    sessionRepo: SessionRepo,
    focusRepo: FocusRepo,
    channelTitles: ChannelTitles,
    channelRepo: ChannelRepo,
    private val pinnedTileRepo: PinnedTileRepo
) : ViewModel() {

    val focusUpdate = focusRepo.focusUpdate
    val focusRequests: Observable<Int> = focusRepo.defaultViewAfterScreenChange
            .filter { it.second == ActiveScreen.NAVIGATION_OVERLAY }
            .map { it.first.viewId }

    @Suppress("DEPRECATION")
    val viewIsSplit: LiveData<Boolean> = sessionRepo.legacyState.map {
        it.currentUrl != URLs.APP_URL_HOME
    }

    // TODO this method is only used by tests. The tests should be updated, and the method removed
    fun unpinPinnedTile(url: String) {
        pinnedTileRepo.removePinnedTile(url)
    }

    val pinnedTiles: Observable<ChannelDetails> = channelRepo.getPinnedTiles()
        .map { ChannelDetails(title = channelTitles.pinned, tileList = it) }

    val newsChannel: Observable<ChannelDetails> = channelRepo.getNewsTiles()
        .map { ChannelDetails(title = channelTitles.newsAndPolitics, tileList = it) }

    val sportsChannel: Observable<ChannelDetails> = channelRepo.getSportsTiles()
        .map { ChannelDetails(title = channelTitles.sports, tileList = it) }

    val musicChannel: Observable<ChannelDetails> = channelRepo.getMusicTiles()
        .map { ChannelDetails(title = channelTitles.music, tileList = it) }

    val shouldDisplayPinnedTiles: Observable<Boolean> = pinnedTiles.map { !it.tileList.isEmpty() }
        .distinctUntilChanged()
}
