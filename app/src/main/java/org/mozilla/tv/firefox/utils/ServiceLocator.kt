/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.search.SearchEngineManager
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ViewModelFactory
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.pocket.PocketEndpoint
import org.mozilla.tv.firefox.pocket.PocketFeedStateMachine
import org.mozilla.tv.firefox.pocket.PocketRepoCache
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.webrender.WebViewCache
import org.mozilla.tv.firefox.session.SessionRepo

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
    private val pocketEndpoint get() = PocketEndpoint(appVersion, buildConfigDerivables.globalPocketVideoEndpoint)
    private val buildConfigDerivables get() = BuildConfigDerivables(getIsEnglishLocale)
    private val pocketFeedStateMachine get() = PocketFeedStateMachine()
    private val getIsEnglishLocale = { LocaleManager.getInstance().currentLanguageIsEnglish(app) }

    val turboMode: TurboMode by lazy { TurboMode(app) }
    val pocketRepoCache by lazy { PocketRepoCache(pocketRepo).apply { unfreeze() } }
    val viewModelFactory by lazy { ViewModelFactory(this, app) }
    val screenControllerStateMachine by lazy {
        ScreenControllerStateMachine { sessionRepo.state.value?.currentUrl == URLs.APP_URL_HOME }
    }
    val screenController by lazy { ScreenController(screenControllerStateMachine) }
    val webViewCache by lazy { WebViewCache(sessionRepo) }
    val sessionManager get() = app.webRenderComponents.sessionManager
    val sessionUseCases get() = app.webRenderComponents.sessionUseCases
    val searchEngineManager = SearchEngineManager().apply { GlobalScope.launch { load(app) } }

    open val pinnedTileRepo by lazy { PinnedTileRepo(app) }
    open val pocketRepo = PocketVideoRepo(pocketEndpoint, pocketFeedStateMachine, getIsEnglishLocale, buildConfigDerivables).apply {
        update()
    }
    open val sessionRepo by lazy { SessionRepo(sessionManager, sessionUseCases, turboMode).apply { observeSources() } }
}
