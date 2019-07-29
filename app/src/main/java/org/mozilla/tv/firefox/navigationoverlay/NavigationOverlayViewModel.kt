/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.channels.ChannelDetails
import org.mozilla.tv.firefox.channels.ChannelRepo
import org.mozilla.tv.firefox.focus.FocusRepo

class ChannelTitles(
    val pinned: String,
    val newsAndPolitics: String,
    val sports: String,
    val music: String,
    val food: String
)

class NavigationOverlayViewModel(
    focusRepo: FocusRepo,
    channelTitles: ChannelTitles,
    channelRepo: ChannelRepo
) : ViewModel() {

    val focusUpdate = focusRepo.focusUpdate
    val focusRequests: Observable<Int> = focusRepo.defaultViewAfterScreenChange
            .filter { it.second == ActiveScreen.NAVIGATION_OVERLAY }
            .map { it.first.viewId }

    val pinnedTiles: Observable<ChannelDetails> = channelRepo.getPinnedTiles()
        .map { ChannelDetails(title = channelTitles.pinned, tileList = it) }

    val newsChannel: Observable<ChannelDetails> = channelRepo.getNewsTiles()
        .map { ChannelDetails(title = channelTitles.newsAndPolitics, tileList = it) }

    val sportsChannel: Observable<ChannelDetails> = channelRepo.getSportsTiles()
        .map { ChannelDetails(title = channelTitles.sports, tileList = it) }

    val musicChannel: Observable<ChannelDetails> = channelRepo.getMusicTiles()
        .map { ChannelDetails(title = channelTitles.music, tileList = it) }

    fun shouldBeDisplayed(channelDetails: Observable<ChannelDetails>): Observable<Boolean> =
        channelDetails.map { it.tileList.isNotEmpty() }
            .distinctUntilChanged()
}
