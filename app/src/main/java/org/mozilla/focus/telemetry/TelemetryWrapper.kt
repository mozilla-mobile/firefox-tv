/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import android.net.http.SslError
import android.os.StrictMode
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.browser.NavigationEvent
import org.mozilla.focus.home.BundledHomeTile
import org.mozilla.focus.home.CustomHomeTile
import org.mozilla.focus.home.HomeTile
import org.mozilla.focus.iwebview.IWebView
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.widget.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

@Suppress(
        // Yes, this a large class with a lot of functions. But it's very simple and still easy to read.
        "TooManyFunctions",
        "LargeClass"
)
object TelemetryWrapper {
    private const val TELEMETRY_APP_NAME_FOCUS_TV = "FirefoxForFireTV"

    private object Category {
        val ACTION = "action"
        val ERROR = "error"
    }

    private object Method {
        val TYPE_URL = "type_url"
        val TYPE_QUERY = "type_query"
        val CLICK = "click"
        val CHANGE = "change"
        val FOREGROUND = "foreground"
        val BACKGROUND = "background"
        val SHOW = "show"
        val HIDE = "hide"
        val PAGE = "page"
        val RESOURCE = "resource"
        val REMOVE = "remove"
    }

    private object Object {
        val SEARCH_BAR = "search_bar"
        val SETTING = "setting"
        val APP = "app"
        val MENU = "menu"
        val BROWSER = "browser"
        const val HOME_TILE = "home_tile"
        val TURBO_MODE = "turbo_mode"
        val PIN_PAGE = "pin_page"
    }

    internal object Value {
        val URL = "url"
        val RELOAD = "refresh"
        val CLEAR_DATA = "clear_data"
        val BACK = "back"
        val FORWARD = "forward"
        val HOME = "home"
        val SETTINGS = "settings"
        val ON = "on"
        val OFF = "off"
        val TILE_BUNDLED = "bundled"
        val TILE_CUSTOM = "custom"
    }

    private object Extra {
        val TO = "to"
        val TOTAL = "total"
        val AUTOCOMPLETE = "autocomplete"
        val SOURCE = "source"
        val ERROR_CODE = "error_code"

        // We need this second source key because we use SOURCE when using this key.
        // For the value, "autocomplete_source" exceeds max extra key length.
        val AUTOCOMPLETE_SOURCE = "autocompl_src"
    }

    @JvmStatic
    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        val threadPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val telemetryEnabled = DataUploadPreference.isEnabled(context)

            val configuration = TelemetryConfiguration(context)
                    .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                    .setAppName(TELEMETRY_APP_NAME_FOCUS_TV)
                    .setUpdateChannel(BuildConfig.BUILD_TYPE)
                    .setPreferencesImportantForTelemetry(
                            IWebView.TRACKING_PROTECTION_ENABLED_PREF,
                            TelemetrySettingsProvider.PREF_CUSTOM_HOME_TILE_COUNT,
                            TelemetrySettingsProvider.PREF_TOTAL_HOME_TILE_COUNT
                    )
                    .setSettingsProvider(TelemetrySettingsProvider(context))
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(telemetryEnabled)

            val serializer = JSONPingSerializer()
            val storage = FileTelemetryStorage(configuration, serializer)
            val client = HttpURLConnectionTelemetryClient()
            val scheduler = JobSchedulerTelemetryScheduler()

            TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                    .addPingBuilder(TelemetryCorePingBuilder(configuration))
                    .addPingBuilder(TelemetryMobileEventPingBuilder(configuration))
                    .setDefaultSearchProvider(createDefaultSearchProvider(context)))
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            SearchEngineManager.getInstance()
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }

    @JvmStatic
    fun startSession() {
        TelemetryHolder.get().recordSessionStart()

        TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    @JvmStatic
    fun stopSession() {
        TelemetryHolder.get().recordSessionEnd()

        TelemetryEvent.create(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
    }

    @JvmStatic
    fun stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryMobileEventPingBuilder.TYPE)
                .scheduleUpload()
    }

    @JvmStatic
    fun urlBarEvent(isUrl: Boolean, autocompleteResult: AutocompleteResult, inputLocation: UrlTextInputLocation) {
        if (isUrl) {
            TelemetryWrapper.browseEvent(autocompleteResult, inputLocation)
        } else {
            TelemetryWrapper.searchEnterEvent(inputLocation)
        }
    }

    private fun browseEvent(autocompleteResult: AutocompleteResult, inputLocation: UrlTextInputLocation) {
        val event = TelemetryEvent.create(Category.ACTION, Method.TYPE_URL, Object.SEARCH_BAR)
                .extra(Extra.AUTOCOMPLETE, (!autocompleteResult.isEmpty).toString())
                .extra(Extra.SOURCE, inputLocation.extra)

        if (!autocompleteResult.isEmpty) {
            event.extra(Extra.TOTAL, autocompleteResult.totalItems.toString())
            event.extra(Extra.AUTOCOMPLETE_SOURCE, autocompleteResult.source)
        }

        event.queue()
    }

    private fun searchEnterEvent(inputLocation: UrlTextInputLocation) {
        val telemetry = TelemetryHolder.get()

        TelemetryEvent.create(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR)
                .extra(Extra.SOURCE, inputLocation.extra)
                .queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

    @JvmStatic
    fun sslErrorEvent(fromPage: Boolean, error: SslError) {
        // SSL Errors from https://developer.android.com/reference/android/net/http/SslError.html
        val primaryErrorMessage = when (error.primaryError) {
            SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
            SslError.SSL_EXPIRED -> "SSL_EXPIRED"
            SslError.SSL_IDMISMATCH -> "SSL_IDMISMATCH"
            SslError.SSL_NOTYETVALID -> "SSL_NOTYETVALID"
            SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
            SslError.SSL_INVALID -> "SSL_INVALID"
            else -> "Undefined SSL Error"
        }
        TelemetryEvent.create(Category.ERROR, if (fromPage) Method.PAGE else Method.RESOURCE, Object.BROWSER)
                .extra(Extra.ERROR_CODE, primaryErrorMessage)
                .queue()
    }

    @JvmStatic
    fun homeTileClickEvent(tile: HomeTile) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE,
                getTileTypeAsStringValue(tile)).queue()
    }

    @JvmStatic
    fun clearDataEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, Value.CLEAR_DATA).queue()
    }

    /** @param isOpening true if the drawer is opening, close otherwise. */
    @JvmStatic
    fun drawerShowHideEvent(isOpening: Boolean) {
        val method = if (isOpening) Method.SHOW else Method.HIDE
        TelemetryEvent.create(Category.ACTION, method, Object.MENU).queue()
    }

    fun overlayClickEvent(event: NavigationEvent, isTurboButtonChecked: Boolean, isPinButtonChecked: Boolean) {
        val telemetryValue = when (event) {
            NavigationEvent.HOME -> Value.HOME
            NavigationEvent.SETTINGS -> Value.SETTINGS

            NavigationEvent.BACK -> Value.BACK
            NavigationEvent.FORWARD -> Value.FORWARD
            NavigationEvent.RELOAD -> Value.RELOAD

            // For legacy reasons, turbo has different telemetry params so we special case it.
            // Pin has a similar state change so we model it after turbo.
            NavigationEvent.TURBO -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.TURBO_MODE, boolToOnOff(isTurboButtonChecked)).queue()
                return
            }
            NavigationEvent.PIN_ACTION -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.PIN_PAGE, boolToOnOff(isPinButtonChecked)).queue()
                return
            }

            // Load is handled in a separate event
            NavigationEvent.LOAD -> return
        }
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, telemetryValue).queue()
    }

    /** The browser goes back from a controller press. */
    @JvmStatic
    fun browserBackControllerEvent() {
        TelemetryEvent.create(Category.ACTION, Method.PAGE, Object.BROWSER, Value.BACK)
                .extra(Extra.SOURCE, "controller")
                .queue()
    }

    fun homeTileRemovedEvent(removedTile: HomeTile) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.HOME_TILE,
                getTileTypeAsStringValue(removedTile)).queue()
    }

    private fun boolToOnOff(boolean: Boolean) = if (boolean) Value.ON else Value.OFF

    private fun getTileTypeAsStringValue(tile: HomeTile) = when (tile) {
        is BundledHomeTile -> Value.TILE_BUNDLED
        is CustomHomeTile -> Value.TILE_CUSTOM
    }
}

enum class UrlTextInputLocation(internal val extra: String) {
    // We hardcode the Strings so we can change the enum
    HOME("home"),
    MENU("menu"),
}
