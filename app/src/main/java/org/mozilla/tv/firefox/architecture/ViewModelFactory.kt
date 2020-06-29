/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.architecture

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.hint.HintContentFactory
import org.mozilla.tv.firefox.navigationoverlay.ChannelTitles
import org.mozilla.tv.firefox.navigationoverlay.NavigationOverlayViewModel
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.navigationoverlay.ToolbarViewModel
import org.mozilla.tv.firefox.settings.SettingsViewModel
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.webrender.WebRenderHintViewModel
import org.mozilla.tv.firefox.webrender.WebRenderViewModel

/**
 * Used by [ViewModelProviders] to instantiate [ViewModel]s with constructor arguments.
 *
 * This should be used through [FirefoxViewModelProviders.of].
 * Example usage:
 * ```kotlin
 * val myViewModel = FirefoxViewModelProviders.of(this).get(ExampleViewModel::class.java)
 * ```
 */
class ViewModelFactory(
    private val serviceLocator: ServiceLocator,
    private val app: Application
) : ViewModelProvider.Factory {

    private val resources = app.resources
    private val hintContentFactory = HintContentFactory(resources)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ToolbarViewModel::class.java -> ToolbarViewModel(
                sessionRepo = serviceLocator.sessionRepo,
                pinnedTileRepo = serviceLocator.pinnedTileRepo
            ) as T

            SettingsViewModel::class.java -> SettingsViewModel(
                serviceLocator.settingsRepo,
                serviceLocator.sessionRepo
            ) as T

            NavigationOverlayViewModel::class.java -> NavigationOverlayViewModel(
                serviceLocator.screenController,
                ChannelTitles(
                    pinned = app.getString(R.string.pinned_tile_channel_title),
                    newsAndPolitics = resources.getString(R.string.news_channel_title),
                    sports = resources.getString(R.string.sports_channel_title),
                    music = resources.getString(R.string.music_channel_title),
                    food = resources.getString(R.string.food_channel_title)
                ),
                serviceLocator.channelRepo,
                ToolbarViewModel(
                        sessionRepo = serviceLocator.sessionRepo,
                        pinnedTileRepo = serviceLocator.pinnedTileRepo
                ),
                serviceLocator.fxaRepo,
                serviceLocator.fxaLoginUseCase
            ) as T

            OverlayHintViewModel::class.java -> OverlayHintViewModel(
                serviceLocator.sessionRepo,
                hintContentFactory.getCloseMenuHint()
            ) as T

            WebRenderHintViewModel::class.java -> WebRenderHintViewModel(
                serviceLocator.sessionRepo,
                serviceLocator.cursorModel,
                serviceLocator.screenController,
                hintContentFactory.getOpenMenuHint()
            ) as T

            WebRenderViewModel::class.java -> WebRenderViewModel(
                serviceLocator.screenController,
                serviceLocator.fxaLoginUseCase
            ) as T

        // This class needs to either return a ViewModel or throw, so we have no good way of silently handling
        // failures in production. However a failure could only occur if code requests a VM that we have not added
        // to this factory, so any problems should be caught in dev.
            else -> throw IllegalArgumentException(
                "A class was passed to ViewModelFactory#create that it does not " +
                    "know how to handle\nClass name: ${modelClass.simpleName}"
            )
        }
    }
}
