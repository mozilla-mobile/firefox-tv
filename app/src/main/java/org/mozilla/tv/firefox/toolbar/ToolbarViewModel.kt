/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.UiThread
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.ext.LiveDataHelper
import org.mozilla.tv.firefox.ext.doOnEach
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.navigationoverlay.BrowserNavigationOverlay
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

    // Values should be pushed to _events using setValue. Two values are set in
    // rapid succession using postValue, only the latest will be received
    private var _events = MutableLiveData<Consumable<BrowserNavigationOverlay.Action>>()
    val events: LiveData<Consumable<BrowserNavigationOverlay.Action>> = _events

    val state: LiveData<ToolbarViewModel.State> =
        LiveDataHelper.combineLatest(sessionRepo.state, pinnedTileRepo.getPinnedTiles()) { sessionState, pinnedTiles ->

            // The menu back button should not be enabled if the previous screen was our initial url (home)
            fun isBackEnabled() = sessionState.backEnabled && sessionState.currentBackForwardIndex > 1
            fun currentUrlIsPinned() = pinnedTiles.containsKey(sessionState.currentUrl)

            ToolbarViewModel.State(
                backEnabled = isBackEnabled(),
                forwardEnabled = sessionState.forwardEnabled,
                refreshEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                pinEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                pinChecked = currentUrlIsPinned(),
                turboChecked = turboMode.isEnabled(),
                desktopModeEnabled = !sessionState.currentUrl.isEqualToHomepage(),
                desktopModeChecked = sessionState.desktopModeActive,
                urlBarText = UrlUtils.toUrlBarDisplay(sessionState.currentUrl)
            )
        }

    init {
        disableDesktopModeWhenHostChanges()
    }

    @UiThread
    fun backButtonClicked() {
        sessionRepo.exitFullScreenIfPossibleAndBack()
        closeOverlay()
    }

    @UiThread
    fun forwardButtonClicked() {
        sessionRepo.goForward()
        closeOverlay()
    }

    @UiThread
    fun reloadButtonClicked() {
        sessionRepo.reload()
        sessionRepo.pushCurrentValue()
        closeOverlay()
    }


    @UiThread
    fun pinButtonClicked() {
        val pinChecked = state.value?.pinChecked ?: return
        val url = sessionRepo.state.value?.currentUrl ?: return

        sendOverlayClickTelemetry(NavigationEvent.PIN_ACTION, pinChecked = !pinChecked)

        if (pinChecked) {
            pinnedTileRepo.removePinnedTile(url)
            _events.value = Consumable.from(BrowserNavigationOverlay.Action.ShowTopToast(R.string.notification_unpinned_site))
        }
        else {
            pinnedTileRepo.addPinnedTile(url, sessionRepo.currentURLScreenshot())
            _events.value = Consumable.from(BrowserNavigationOverlay.Action.ShowTopToast(R.string.notification_pinned_site))
        }
        closeOverlay()
    }

    @UiThread
    fun turboButtonClicked() {
        turboMode.setEnabled(!turboMode.isEnabled())
        sessionRepo.reload()

        sendOverlayClickTelemetry(NavigationEvent.TURBO, turboChecked = turboMode.isEnabled())
        sessionRepo.state.value?.currentUrl?.let { if (!it.isEqualToHomepage()) closeOverlay() }
    }

    @UiThread
    fun desktopModeButtonClicked() {
        val previouslyChecked = state.value?.desktopModeChecked ?: return

        sendOverlayClickTelemetry(NavigationEvent.DESKTOP_MODE, desktopModeChecked = !previouslyChecked)

        sessionRepo.setDesktopMode(!previouslyChecked)
        val textId = when {
            previouslyChecked -> R.string.notification_request_non_desktop_site
            else -> R.string.notification_request_desktop_site
        }

        _events.value = Consumable.from(BrowserNavigationOverlay.Action.ShowBottomToast(textId))
        closeOverlay()
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

    private fun closeOverlay() {
        _events.value = Consumable.from(BrowserNavigationOverlay.Action.SetOverlayVisible(false))
    }

    private fun String.isEqualToHomepage() = this == AppConstants.APP_URL_HOME

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
