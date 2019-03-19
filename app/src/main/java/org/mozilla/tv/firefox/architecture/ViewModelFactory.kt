/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.architecture

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.mozilla.tv.firefox.navigationoverlay.NavigationOverlayViewModel
import org.mozilla.tv.firefox.navigationoverlay.OverlayHintViewModel
import org.mozilla.tv.firefox.pinnedtile.PinnedTileViewModel
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.navigationoverlay.ToolbarViewModel
import org.mozilla.tv.firefox.settings.SettingsViewModel
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.webrender.WebRenderHintViewModel
import org.mozilla.tv.firefox.webrender.cursor.CursorViewModel

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

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PinnedTileViewModel::class.java -> PinnedTileViewModel(serviceLocator.pinnedTileRepo) as T

            PocketViewModel::class.java -> PocketViewModel(
                serviceLocator.pocketRepo,
                serviceLocator.pocketRepoCache
            ) as T

            ToolbarViewModel::class.java -> ToolbarViewModel(
                sessionRepo = serviceLocator.sessionRepo,
                pinnedTileRepo = serviceLocator.pinnedTileRepo
            ) as T

            SettingsViewModel::class.java -> SettingsViewModel(
                serviceLocator.frameworkRepo,
                serviceLocator.settingsRepo,
                serviceLocator.sessionRepo
            ) as T

            CursorViewModel::class.java -> CursorViewModel(
                serviceLocator.frameworkRepo,
                serviceLocator.screenController,
                serviceLocator.sessionRepo
            ) as T

            NavigationOverlayViewModel::class.java -> NavigationOverlayViewModel(
                    serviceLocator.sessionRepo
            ) as T

            OverlayHintViewModel::class.java -> OverlayHintViewModel(
                    serviceLocator.sessionRepo
            ) as T

            WebRenderHintViewModel::class.java -> WebRenderHintViewModel(
                    serviceLocator.sessionRepo
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
