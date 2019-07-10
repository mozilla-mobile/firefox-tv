/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// We want the Pocket code from a-c: #1976. Unfortunately, the compiler won't let
// us suppress individual lines so we have to suppress the file.
@file:Suppress("DEPRECATION")

package org.mozilla.tv.firefox.utils

import android.app.Application
import androidx.lifecycle.MutableLiveData
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.fxa.FxaLoginUseCase
import org.mozilla.tv.firefox.fxa.FxaRepo
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ValidatedIntentData
import org.mozilla.tv.firefox.architecture.ViewModelFactory
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.experiments.ExperimentsProvider
import org.mozilla.tv.firefox.experiments.FretboardProvider
import org.mozilla.tv.firefox.ext.getAccessibilityManager
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.focus.FocusRepo
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.channels.ChannelRepo
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileImageUtilWrapper
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.pocket.PocketEndpointRaw
import org.mozilla.tv.firefox.pocket.PocketVideoFetchScheduler
import org.mozilla.tv.firefox.pocket.PocketVideoJSONValidator
import org.mozilla.tv.firefox.pocket.PocketVideoParser
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketVideoStore
import org.mozilla.tv.firefox.search.SearchEngineManagerFactory
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.settings.SettingsRepo
import org.mozilla.tv.firefox.webrender.EngineViewCache
import org.mozilla.tv.firefox.webrender.cursor.CursorModel

/**
 * Implementation of the Service Locator pattern. Use this class to provide dependencies without
 * making client code aware of their specific implementations (i.e., make it easier to program to
 * an interface).
 *
 * This also makes it easier to mock out dependencies during testing.
 *
 * See: https://en.wikipedia.org/wiki/Service_locator_pattern
 *
 * ### Dependencies can be defined as follows:
 *
 *   #### Lazy, app-wide Singleton:
 *   ```
 *   open val pocket by lazy { Pocket() }
 *   ```
 *
 *   #### Eager, app-wide singleton:
 *   ```
 *   open val pocket = Pocket()
 *   ```
 *
 *   #### New value each time:
 *   ```
 *   open val pocket: Pocket get() = Pocket()
 *   ```
 *
 *   #### Concrete value for interface:
 *   ```
 *   open val telemetry: TelemetryInterface by lazy { SentryWrapper() }
 *   ```
 */
open class ServiceLocator(val app: Application) {
    private val appVersion = app.packageManager.getPackageInfo(app.packageName, 0).versionName
    private val buildConfigDerivables get() = BuildConfigDerivables()
    private val isPocketEnabledByLocale = { LocaleManager.getInstance().currentLanguageIsEnglish(app) } // Pocket is en-US only.
    private val pocketVideoParser by lazy { PocketVideoParser }
    private val pocketVideoJSONValidator by lazy { PocketVideoJSONValidator(pocketVideoParser) }

    val intentLiveData by lazy { MutableLiveData<Consumable<ValidatedIntentData?>>() }
    val fretboardProvider: FretboardProvider by lazy { FretboardProvider(app) }
    val experimentsProvider by lazy { ExperimentsProvider(fretboardProvider.fretboard, app) }
    val turboMode: TurboMode by lazy { TurboMode(app) }
    val viewModelFactory by lazy { ViewModelFactory(this, app) }
    val screenController by lazy { ScreenController(sessionRepo) }
    val engineViewCache by lazy { EngineViewCache(sessionRepo) }
    val sessionManager get() = app.webRenderComponents.sessionManager
    val sessionUseCases get() = app.webRenderComponents.sessionUseCases
    val searchEngineManager by lazy { SearchEngineManagerFactory.create(app) }
    val cursorModel by lazy { CursorModel(screenController.currentActiveScreen, frameworkRepo, sessionRepo) }
    val focusRepo by lazy { FocusRepo(screenController, sessionRepo, pinnedTileRepo, pocketRepo) }
    val screenshotStoreWrapper by lazy { PinnedTileImageUtilWrapper(app) }
    val formattedDomainWrapper by lazy { FormattedDomainWrapper(app) }
    val channelRepo by lazy { ChannelRepo(app, screenshotStoreWrapper, formattedDomainWrapper, pinnedTileRepo) }
    @Suppress("DEPRECATION") // We need PocketEndpointRaw until we move to a-c's impl.
    val pocketEndpointRaw by lazy { PocketEndpointRaw(appVersion, buildConfigDerivables.globalPocketVideoEndpoint) }
    val pocketVideoStore by lazy { PocketVideoStore(app, app.assets, pocketVideoJSONValidator) }
    val pocketVideoFetchScheduler by lazy { PocketVideoFetchScheduler(isPocketEnabledByLocale) }
    val fxaRepo by lazy { FxaRepo(app) }
    val fxaLoginUseCase by lazy { FxaLoginUseCase(fxaRepo, sessionRepo, screenController) }

    // These open vals are overridden in testing
    open val frameworkRepo = FrameworkRepo.newInstanceAndInit(app.getAccessibilityManager())
    open val pinnedTileRepo by lazy { PinnedTileRepo(app) }
    open val pocketRepo by lazy { PocketVideoRepo.newInstance(
        pocketVideoStore,
        isPocketEnabledByLocale,
        buildConfigDerivables.isPocketKeyValid
    ) }
    open val sessionRepo by lazy { SessionRepo(sessionManager, sessionUseCases, turboMode).apply { observeSources() } }
    open val settingsRepo by lazy { SettingsRepo(app) }
}
