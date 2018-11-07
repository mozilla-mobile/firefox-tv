/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.ext.LiveDataHelper
import org.mozilla.tv.firefox.utils.AppConstants
import org.mozilla.tv.firefox.utils.TurboMode
import org.mozilla.tv.firefox.utils.UrlUtils

open class ToolbarViewModel(
    sessionRepo: SessionRepo,
    pinnedTileRepo: PinnedTileRepo,
    turboMode: TurboMode
) : ViewModel() {

    data class State(
        val backEnabled: Boolean,
        val forwardEnabled: Boolean,
        val refreshEnabled: Boolean,
        val pinEnabled: Boolean,
        val pinChecked: Boolean,
        val turboChecked: Boolean,
        val desktopModeEnabled: Boolean,
        val desktopModeChecked: Boolean,
        val urlBarText: String
    )

    val state: LiveData<ToolbarViewModel.State> =
        LiveDataHelper.combineLatest(sessionRepo.state, pinnedTileRepo.getPinnedTiles()) { sessionState, pinnedTiles ->

            fun isUrlEqualToHomepage() = sessionState.currentUrl == AppConstants.APP_URL_HOME
            fun currentUrlIsPinned() = pinnedTiles.containsKey(sessionState.currentUrl)

            ToolbarViewModel.State(
                backEnabled = sessionState.backEnabled,
                forwardEnabled = sessionState.forwardEnabled,
                refreshEnabled = !isUrlEqualToHomepage(),
                pinEnabled = !isUrlEqualToHomepage(),
                pinChecked = currentUrlIsPinned(),
                turboChecked = turboMode.isEnabled(),
                desktopModeEnabled = !isUrlEqualToHomepage(),
                desktopModeChecked = sessionState.desktopModeActive,
                urlBarText = UrlUtils.toUrlBarDisplay(sessionState.currentUrl)
            )
        }
}
