/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.ext.LiveDataHelper
import org.mozilla.tv.firefox.utils.AppConstants
import org.mozilla.tv.firefox.utils.TurboMode
import org.mozilla.tv.firefox.utils.UrlUtils

open class ToolbarViewModel(
    private val turboMode: TurboMode,
    private val sessionUseCases: SessionUseCases,
    private val sessionRepo: SessionRepo,
    private val pinnedTileRepo: PinnedTileRepo
) : ViewModel() {

    data class State(
        val backEnabled: Boolean,
        val forwardEnabled: Boolean,
        val refreshEnabled: Boolean,
        val pinEnabled: Boolean,
        val pinChecked: Boolean, // TODO update these to CheckedState
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

    fun turboButtonClicked() {
        turboMode.setEnabled(!turboMode.isEnabled())
        sessionUseCases.reload.invoke()
    }

    /**
     * Returns true if the pin button will now be checked
     */
    fun pinButtonClicked(): Boolean? {
        val vmState = state.value ?: return null
        val url = sessionRepo.state.value?.currentUrl ?: return null

        return if (vmState.pinChecked) {
            pinnedTileRepo.removePinnedTile(url)
            false
        } else {
            pinnedTileRepo.addPinnedTile(url, sessionRepo.currentURLScreenshot())
            true
        }
    }
}
