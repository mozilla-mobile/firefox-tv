/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.ext.LiveDataCombiners
import org.mozilla.tv.firefox.navigationoverlay.NavigationEvent
import org.mozilla.tv.firefox.navigationoverlay.NavigationOverlayFragment
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.SetOnlyLiveData
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.UrlUtils

class ToolbarViewModel(
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

    // Values should be pushed to _events using setValue. Two values are set in
    // rapid succession using postValue, only the latest will be received
    private var _events = SetOnlyLiveData<Consumable<NavigationOverlayFragment.Action>>()
    // Note that events will only emit values if state is observed
    val events: LiveData<Consumable<NavigationOverlayFragment.Action>> = _events

    val state: LiveData<ToolbarViewModel.State> =
        LiveDataCombiners.combineLatest(sessionRepo.state, pinnedTileRepo.getPinnedTiles()) { sessionState, pinnedTiles ->

            fun isCurrentURLPinned() = pinnedTiles.containsKey(sessionState.currentUrl)

            ToolbarViewModel.State(
                backEnabled = sessionState.backEnabled,
                forwardEnabled = sessionState.forwardEnabled,
                refreshEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                pinEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                pinChecked = isCurrentURLPinned(),
                turboChecked = sessionState.turboModeActive,
                desktopModeEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                desktopModeChecked = sessionState.desktopModeActive,
                urlBarText = UrlUtils.toUrlBarDisplay(sessionState.currentUrl)
            )
        }

    @UiThread
    fun backButtonClicked() {
        sessionRepo.exitFullScreenIfPossibleAndBack()
        hideOverlay()
    }

    @UiThread
    fun forwardButtonClicked() {
        sessionRepo.goForward()
        hideOverlay()
    }

    @UiThread
    fun reloadButtonClicked() {
        sessionRepo.reload()
        sessionRepo.pushCurrentValue()
        hideOverlay()
    }

    @UiThread
    fun pinButtonClicked() {
        val pinChecked = state.value?.pinChecked ?: return
        val url = sessionRepo.state.value?.currentUrl ?: return

        sendOverlayClickTelemetry(NavigationEvent.PIN_ACTION, pinChecked = !pinChecked)

        if (pinChecked) {
            pinnedTileRepo.removePinnedTile(url)
            _events.value = Consumable.from(NavigationOverlayFragment.Action.ShowTopToast(R.string.notification_unpinned_site))
        } else {
            pinnedTileRepo.addPinnedTile(url, sessionRepo.currentURLScreenshot())
            _events.value = Consumable.from(NavigationOverlayFragment.Action.ShowTopToast(R.string.notification_pinned_site))
        }
        hideOverlay()
    }

    @UiThread
    fun turboButtonClicked() {
        val currentUrl = sessionRepo.state.value?.currentUrl
        val turboModeActive = sessionRepo.state.value?.turboModeActive ?: true

        sessionRepo.setTurboModeEnabled(!turboModeActive)
        sessionRepo.reload()

        sendOverlayClickTelemetry(NavigationEvent.TURBO, turboChecked = !turboModeActive)
        currentUrl?.let { if (!it.isEqualToHomepage()) hideOverlay() }
    }

    @UiThread
    fun desktopModeButtonClicked() {
        val desktopModeChecked = state.value?.desktopModeChecked ?: return

        sendOverlayClickTelemetry(NavigationEvent.DESKTOP_MODE, desktopModeChecked = !desktopModeChecked)

        sessionRepo.setDesktopMode(!desktopModeChecked)
        val textId = when {
            desktopModeChecked -> R.string.notification_request_non_desktop_site
            else -> R.string.notification_request_desktop_site
        }

        _events.value = Consumable.from(NavigationOverlayFragment.Action.ShowBottomToast(textId))
        hideOverlay()
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

    private fun String.isEqualToHomepage() = this == URLs.APP_URL_HOME

    private fun hideOverlay() {
        _events.value = Consumable.from(NavigationOverlayFragment.Action.SetOverlayVisible(false))
    }
}
