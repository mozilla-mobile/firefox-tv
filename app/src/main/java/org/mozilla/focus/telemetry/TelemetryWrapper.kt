/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import android.net.http.SslError
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.annotation.CheckResult
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.widget.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.measurement.SettingsMeasurement
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
        val SAVE = "save"
        val OPEN = "open"
        val INSTALL = "install"
        val SHOW = "show"
        val HIDE = "hide"
        val REMOVE = "remove"
        val REORDER = "reorder"
        val RESTORE = "restore"
        val PAGE = "page"
        val RESOURCE = "resource"
    }

    private object Object {
        val SEARCH_BAR = "search_bar"
        val SETTING = "setting"
        val APP = "app"
        val MENU = "menu"
        val NOTIFICATION_ACTION = "notification_action"
        val SHORTCUT = "shortcut"
        val BLOCKING_SWITCH = "blocking_switch"
        val BROWSER = "browser"
        val ADD_TO_HOMESCREEN_DIALOG = "add_to_homescreen_dialog"
        val APP_ICON = "app_icon"
        val AUTOCOMPLETE_DOMAIN = "autocomplete_domain"
        val SEARCH_ENGINE_SETTING = "search_engine_setting"
        val ADD_SEARCH_ENGINE_LEARN_MORE = "search_engine_learn_more"
        val CUSTOM_SEARCH_ENGINE = "custom_search_engine"
        val REMOVE_SEARCH_ENGINES = "remove_search_engines"
        const val HOME_TILE = "home_tile"
        val TURBO_MODE = "turbo_mode"
    }

    internal object Value {
        val FIREFOX = "firefox"
        val ERASE = "erase"
        val ERASE_AND_OPEN = "erase_open"
        val CUSTOM_TAB = "custom_tab"
        val OPEN = "open"
        val URL = "url"
        val CANCEL = "cancel"
        val ADD_TO_HOMESCREEN = "add_to_homescreen"
        val RESUME = "resume"
        val RELOAD = "refresh"
        val CLEAR_DATA = "clear_data"
        val BACK = "back"
        val FORWARD = "forward"
        val HOME = "home"
        val SETTINGS = "settings"
        val ON = "on"
        val OFF = "off"
    }

    private object Extra {
        val FROM = "from"
        val TO = "to"
        val TOTAL = "total"
        val SELECTED = "selected"
        val AUTOCOMPLETE = "autocomplete"
        val SOURCE = "source"
        val SUCCESS = "success"
        val ERROR_CODE = "error_code"

        // We need this second source key because we use SOURCE when using this key.
        // For the value, "autocomplete_source" exceeds max extra key length.
        val AUTOCOMPLETE_SOURCE = "autocompl_src"
    }

    @JvmStatic
    fun isTelemetryEnabled(context: Context): Boolean {
        if (AppConstants.isDevBuild()) return false

        // The first access to shared preferences will require a disk read.
        val threadPolicy = StrictMode.allowThreadDiskReads()
        try {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            return preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), true)
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    @JvmStatic
    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        preferences.edit()
                .putBoolean(resources.getString(R.string.pref_key_telemetry), enabled)
                .apply()

        TelemetryHolder.get()
                .configuration
                .setUploadEnabled(enabled).isCollectionEnabled = enabled
    }

    @JvmStatic
    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        val threadPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val resources = context.resources

            val telemetryEnabled = isTelemetryEnabled(context)

            val configuration = TelemetryConfiguration(context)
                    .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                    .setAppName(TELEMETRY_APP_NAME_FOCUS_TV)
                    .setUpdateChannel(BuildConfig.BUILD_TYPE)
                    .setPreferencesImportantForTelemetry(
                            IWebView.TRACKING_PROTECTION_ENABLED_PREF
                    )
                    .setSettingsProvider(SettingsMeasurement.SharedPreferenceSettingsProvider())
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

    /**
     * Add the position of the current session and total number of sessions as extras to the event
     * and return it.
     */
    @CheckResult
    private fun withSessionCounts(event: TelemetryEvent): TelemetryEvent {
        val sessionManager = SessionManager.getInstance()

        event.extra(Extra.SELECTED, sessionManager.positionOfCurrentSession.toString())
        event.extra(Extra.TOTAL, sessionManager.numberOfSessions.toString())

        return event
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

    @JvmStatic
    fun customTabMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.CUSTOM_TAB).queue()
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
    fun eraseAndOpenNotificationActionEvent() {
        withSessionCounts(TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.NOTIFICATION_ACTION,
                Value.ERASE_AND_OPEN)
        ).queue()
    }

    @JvmStatic
    fun openNotificationActionEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.NOTIFICATION_ACTION, Value.OPEN).queue()
    }

    @JvmStatic
    fun addToHomescreenShortcutEvent() {
        TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.ADD_TO_HOMESCREEN_DIALOG,
                Value.ADD_TO_HOMESCREEN
        ).queue()
    }

    @JvmStatic
    fun cancelAddToHomescreenShortcutEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.ADD_TO_HOMESCREEN_DIALOG, Value.CANCEL).queue()
    }

    @JvmStatic
    fun eraseShortcutEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.SHORTCUT, Value.ERASE))
                .queue()
    }

    @JvmStatic
    fun settingsEvent(key: String, value: String) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .queue()
    }

    @JvmStatic
    fun openFromIconEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.APP_ICON, Value.OPEN).queue()
    }

    @JvmStatic
    fun resumeFromIconEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.APP_ICON, Value.RESUME).queue()
    }

    @JvmStatic
    fun installFirefoxEvent() {
        TelemetryEvent.create(Category.ACTION, Method.INSTALL, Object.APP, Value.FIREFOX).queue()
    }

    @JvmStatic
    fun blockingSwitchEvent(isBlockingEnabled: Boolean) {
        TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.BLOCKING_SWITCH,
                isBlockingEnabled.toString()
        ).queue()
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

    fun saveAutocompleteDomainEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.AUTOCOMPLETE_DOMAIN).queue()
    }

    fun removeAutocompleteDomainsEvent(count: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.AUTOCOMPLETE_DOMAIN)
                .extra(Extra.TOTAL, count.toString())
                .queue()
    }

    fun reorderAutocompleteDomainEvent(from: Int, to: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REORDER, Object.AUTOCOMPLETE_DOMAIN)
                .extra(Extra.FROM, from.toString())
                .extra(Extra.TO, to.toString())
                .queue()
    }

    @JvmStatic
    fun setDefaultSearchEngineEvent(source: String) {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.SEARCH_ENGINE_SETTING)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @JvmStatic
    fun openSearchSettingsEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuRemoveEnginesEvent() {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuRestoreEnginesEvent() {
        TelemetryEvent.create(Category.ACTION, Method.RESTORE, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuAddSearchEngineEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.CUSTOM_SEARCH_ENGINE).queue()
    }

    @JvmStatic
    fun saveCustomSearchEngineEvent(success: Boolean) {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.CUSTOM_SEARCH_ENGINE)
                .extra(Extra.SUCCESS, success.toString())
                .queue()
    }

    @JvmStatic
    fun removeSearchEnginesEvent(selected: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.REMOVE_SEARCH_ENGINES)
                .extra(Extra.SELECTED, selected.toString())
                .queue()
    }

    @JvmStatic
    fun addSearchEngineLearnMoreEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.ADD_SEARCH_ENGINE_LEARN_MORE).queue()
    }

    @JvmStatic
    fun homeTileClickEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE).queue()
    }

    @JvmStatic
    fun clearDataEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, Value.CLEAR_DATA).queue()
    }

    /** @param isShowing true if the drawer is opening, close otherwise. */
    @JvmStatic
    fun drawerShowHideEvent(isShowing: Boolean) {
        val method = if (isShowing) Method.SHOW else Method.HIDE
        TelemetryEvent.create(Category.ACTION, method, Object.MENU).queue()
    }

    @JvmStatic
    fun menuBrowserNavEvent(button: MenuBrowserNavButton) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, button.value).queue()
    }

    @JvmStatic
    fun menuAppNavEvent(button: MenuAppNavButton) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, button.value).queue()
    }

    /**
     * The browser goes back from a controller press. Another way to track back presses is
     * [menuBrowserNavEvent] with [MenuBrowserNavButton.BACK].
     */
    @JvmStatic
    fun browserBackControllerEvent() {
        TelemetryEvent.create(Category.ACTION, Method.PAGE, Object.BROWSER, Value.BACK)
                .extra(Extra.SOURCE, "controller")
                .queue()
    }

    /** @param isSwitchedOn true if the switch was turned on, false otherwise. */
    @JvmStatic
    fun turboModeSwitchEvent(isSwitchedOn: Boolean) {
        val value = if (isSwitchedOn) Value.ON else Value.OFF
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.TURBO_MODE, value).queue()
    }
}

enum class UrlTextInputLocation(internal val extra: String) {
    // We hardcode the Strings so we can change the enum
    HOME("home"),
    MENU("menu"),
}

enum class MenuBrowserNavButton(val value: String) {
    // We define separate `value`s so we can rename the enum without interfering.
    REFRESH(TelemetryWrapper.Value.RELOAD),
    BACK(TelemetryWrapper.Value.BACK),
    FORWARD(TelemetryWrapper.Value.FORWARD),
}

enum class MenuAppNavButton(val value: String) {
    // We define separate `value`s so we can rename the enum without interfering.
    HOME(TelemetryWrapper.Value.HOME),
    SETTINGS(TelemetryWrapper.Value.SETTINGS),
}
