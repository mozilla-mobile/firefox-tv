/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.navigationoverlay.channels.ChannelDetails
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.focus.FocusRepo
import org.mozilla.tv.firefox.navigationoverlay.channels.ChannelTile
import org.mozilla.tv.firefox.pinnedtile.PinnedTileImageUtilWrapper
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import org.mozilla.tv.firefox.utils.URLs

class NavigationOverlayViewModel(
    sessionRepo: SessionRepo,
    focusRepo: FocusRepo,
    imageUtilityWrapper: PinnedTileImageUtilWrapper,
    formattedDomainWrapper: FormattedDomainWrapper,
    pinnedChannelTitle: String,
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

    // This method converts our existing pinned tiles implementation to the new channel
    // style. When possible, we should update the repo to provide the correct type,
    // rather than converting here
    val pinnedTiles: Observable<ChannelDetails> = pinnedTileRepo.pinnedTiles
            .observeOn(Schedulers.computation())
            // This takes place off of the main thread because PinnedTile.toChannelTile needs
            // to perform file access, and blocks to do so
            .map { it.values.map { it.toChannelTile(imageUtilityWrapper, formattedDomainWrapper) } }
            .map { ChannelDetails(title = pinnedChannelTitle, tileList = it) } // TODO extract string
            .observeOn(AndroidSchedulers.mainThread())

    val shouldDisplayPinnedTiles: Observable<Boolean> = pinnedTiles.map { !it.tileList.isEmpty() }
            .distinctUntilChanged()

    fun unpinPinnedTile(url: String) {
        pinnedTileRepo.removePinnedTile(url)
    }

    val mozillaChannelData = Observable.just(
            ChannelDetails(
                    title = "Mozilla",
                    tileList = listOf(
                            ChannelTile(
                                    "https:www.mozilla.org",
                                    "Mozilla 1",
                                    // Subclassing ChannelTile to simplify setting the image for your use case is encouraged.
                                    // See BundledPinnedTile#toChannelTile and CustomPinnedTile#BundledPinnedTile for how it's
                                    // currently handled
                                    { imageView -> imageView.setImageResource(R.drawable.ic_pocket_and_wordmark) }
                            ),
                            ChannelTile(
                                    "https:www.mozilla.org",
                                    "Mozilla 2",
                                    { imageView -> imageView.setImageResource(R.drawable.ic_pocket_and_wordmark) }
                            ),
                            ChannelTile(
                                    "https:www.mozilla.org",
                                    "Mozilla 3",
                                    { imageView -> imageView.setImageResource(R.drawable.ic_pocket_and_wordmark) }
                            ),
                            ChannelTile(
                                    "https:www.mozilla.org",
                                    "Mozilla 4",
                                    { imageView -> imageView.setImageResource(R.drawable.ic_pocket_and_wordmark) }
                            )
                    )
            )
    )

    val shouldDisplayMozillaChannel = mozillaChannelData.map { it.tileList.isNotEmpty() }
            .distinctUntilChanged()
}
