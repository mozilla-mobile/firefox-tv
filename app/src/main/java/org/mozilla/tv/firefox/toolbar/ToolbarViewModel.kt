/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.ext.LiveDataHelper
import org.mozilla.tv.firefox.ext.doOnEach
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.navigationoverlay.NavigationEvent
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.AppConstants
import org.mozilla.tv.firefox.utils.TurboMode
import org.mozilla.tv.firefox.utils.UrlUtils

open class ToolbarViewModel(
    private val turboMode: TurboMode,
    private val sessionRepo: SessionRepo,
    private val pinnedTileRepo: PinnedTileRepo,
    private val telemetryIntegration: TelemetryIntegration = TelemetryIntegration.INSTANCE
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

    private var previousURLHost: String? = null

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

    init {
        disableDesktopModeWhenHostChanges()
    }

    fun backButtonClicked() = sessionRepo.exitFullScreenIfPossibleAndBack()

    fun forwardButtonClicked() = sessionRepo.goForward()

    fun reloadButtonClicked() {
        sessionRepo.reload()
        sessionRepo.pushCurrentValue()
    }

    /**
     * Returns true if the pin button will now be checked
     */
    fun pinButtonClicked(): Boolean? {
        val pinChecked = state.value?.pinChecked ?: return null
        val url = sessionRepo.state.value?.currentUrl ?: return null

        sendOverlayClickTelemetry(NavigationEvent.PIN_ACTION, pinChecked = !pinChecked)

        if (pinChecked) pinnedTileRepo.removePinnedTile(url)
        else pinnedTileRepo.addPinnedTile(url, sessionRepo.currentURLScreenshot())

        return !pinChecked
    }

    fun turboButtonClicked() {
        turboMode.setEnabled(!turboMode.isEnabled())
        sessionRepo.reload()

        sendOverlayClickTelemetry(NavigationEvent.TURBO, turboChecked = turboMode.isEnabled())
    }

    /**
     * Returns true if the desktop mode button will now be checked
     */
    fun desktopModeButtonClicked(): Boolean? {
        val previouslyChecked = state.value?.desktopModeChecked ?: return null

        sendOverlayClickTelemetry(NavigationEvent.DESKTOP_MODE, desktopModeChecked = !previouslyChecked)

        sessionRepo.setDesktopMode(!previouslyChecked)
        return !previouslyChecked
    }

    private fun sendOverlayClickTelemetry(
        event: NavigationEvent,
        turboChecked: Boolean? = null,
        pinChecked: Boolean? = null,
        desktopModeChecked: Boolean? = null
    ) {
        state.value?.let {
            telemetryIntegration.overlayClickEvent(
                event,
                turboChecked ?: it.turboChecked,
                pinChecked ?: it.pinChecked,
                desktopModeChecked ?: it.desktopModeChecked
            )
        }
    }

    private fun disableDesktopModeWhenHostChanges() {
        sessionRepo.state.observeForever {
            it ?: return@observeForever

            fun isURLHostChanging(): Boolean {
                val currentURLHost = it.currentUrl.toUri()?.host ?: return true

                return (previousURLHost != currentURLHost).also {
                    previousURLHost = currentURLHost
                }
            }

            if (isURLHostChanging() && it.desktopModeActive) {
                sessionRepo.setDesktopMode(false)
                it.currentUrl.toUri()?.let { sessionRepo.loadURL(it) }
            }
        }
    }
}
