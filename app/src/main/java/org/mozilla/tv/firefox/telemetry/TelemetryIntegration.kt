/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.content.Context
import android.net.http.SslError
import android.os.StrictMode
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import android.view.InputDevice
import android.view.KeyEvent
import mozilla.components.support.ktx.android.os.resetAfter
// Glean
// import mozilla.components.service.glean.BuildConfig
// import mozilla.components.service.glean.Glean
import org.mozilla.tv.firefox.GleanMetrics.App
import org.mozilla.tv.firefox.GleanMetrics.Browser
import org.mozilla.tv.firefox.GleanMetrics.Video
import org.mozilla.tv.firefox.GleanMetrics.Tiles
import org.mozilla.tv.firefox.GleanMetrics.Setting
import org.mozilla.tv.firefox.GleanMetrics.Nav
import org.mozilla.tv.firefox.GleanMetrics.Toggles
import org.mozilla.tv.firefox.GleanMetrics.Pin
import org.mozilla.tv.firefox.GleanMetrics.Pocket
// End of glean dependencies
import org.mozilla.tv.firefox.navigationoverlay.NavigationEvent
import org.mozilla.tv.firefox.utils.Assert
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.ping.TelemetryPocketEventPingBuilder
// import org.mozilla.telemetry.glean.Glean
// import org.mozilla.tv.firefox.GleanMetrics
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.channels.ChannelTile
import org.mozilla.tv.firefox.channels.SettingsButton
import org.mozilla.tv.firefox.channels.SettingsScreen
import org.mozilla.tv.firefox.channels.SettingsTile
import org.mozilla.tv.firefox.channels.TileSource
import java.util.Collections

private const val SHARED_PREFS_KEY = "telemetryLib" // Don't call it TelemetryWrapper to avoid accidental IDE rename.
private const val KEY_CLICKED_HOME_TILE_IDS_PER_SESSION = "clickedHomeTileIDsPerSession"
private const val KEY_REMOTE_CONTROL_NAME = "remoteControlName"
private const val YOUTUBE_TILE_ID = "youtube"

@Suppress(
        // Yes, this a large class with a lot of functions. But it's very simple and still easy to read.
        "TooManyFunctions",
        "LargeClass"
)
open class TelemetryIntegration protected constructor(
    private val sentryIntegration: SentryIntegration = SentryIntegration
) {

    companion object {
        val INSTANCE: TelemetryIntegration by lazy { TelemetryIntegration() }
    }

    private object Category {
        const val ACTION = "action"
        const val AGGREGATE = "aggregate"
        const val ERROR = "error"
        const val POCKET = "pocket"
    }

    private object Method {
        const val TYPE_URL = "type_url"
        const val TYPE_QUERY = "type_query"
        const val CLICK = "click"
        const val CLICK_OR_VOICE = "click_or_voice"
        const val CHANGE = "change"
        const val FOREGROUND = "foreground"
        const val BACKGROUND = "background"
        const val USER_SHOW = "user_show"
        const val PAGE = "page"
        const val RESOURCE = "resource"
        const val REMOVE = "remove"
        const val NO_ACTION_TAKEN = "no_action_taken"
        const val YOUTUBE_CAST = "youtube_cast"
        const val VIEW_INTENT = "view_intent"
        const val IMPRESSION = "impression"
        const val PROGRAMMATICALLY_CLOSED = "programmatically_closed"
    }

    private object Object {
        const val SEARCH_BAR = "search_bar"
        const val SETTING = "setting"
        const val APP = "app"
        const val MENU = "menu"
        const val BROWSER = "browser"
        const val HOME_TILE = "home_tile"
        const val TURBO_MODE = "turbo_mode"
        const val PIN_PAGE = "pin_page"
        const val POCKET_VIDEO = "pocket_video"
        const val MEDIA_SESSION = "media_session"
        const val DESKTOP_MODE = "desktop_mode"
        const val VIDEO_ID = "video_id"
        const val FULL_SCREEN_VIDEO = "full_screen_video"
        const val FXA = "fxa"
    }

    internal object Value {
        const val URL = "url"
        const val RELOAD = "refresh"
        const val CLEAR_DATA = "clear_data"
        const val BACK = "back"
        const val FORWARD = "forward"
        const val ON = "on"
        const val OFF = "off"
        const val TILE_BUNDLED = "bundled"
        const val TILE_CUSTOM = "custom"
        const val TILE_POCKET = "pocket"
        const val YOUTUBE_TILE = "youtube_tile"
        const val EXIT_FIREFOX = "exit"
        const val SETTINGS_CLEAR_DATA_TILE = "clear_data_tile"
        const val SETTINGS_SEND_DATA_TILE = "send_data_tile"
        const val SETTINGS_ABOUT_TILE = "about_tile"
        const val SETTINGS_PRIVACY_TILE = "privacy_tile"
        const val FXA_LOGIN_BUTTON = "fxa_login_button"
    }

    private object Extra {
        const val TOTAL = "total"
        const val AUTOCOMPLETE = "autocomplete"
        const val SOURCE = "source"
        const val ERROR_CODE = "error_code"

        // We need this second source key because we use SOURCE when using this key.
        // For the const value, "autocomplete_source" exceeds max extra key length.
        const val AUTOCOMPLETE_SOURCE = "autocompl_src"
        const val TILE_ID = "tile_id"
    }

    // Available on any thread: we synchronize.
    private val pocketUniqueClickedVideoIDs = Collections.synchronizedSet(mutableSetOf<String>())
    private val pocketUniqueImpressedVideoIDs = Collections.synchronizedSet(mutableSetOf<String>())

    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        StrictMode.allowThreadDiskWrites().resetAfter {
            TelemetryHolder.set(TelemetryFactory.createTelemetry(context))
        }
    }

    val clientId: String
        get() = TelemetryHolder.get().clientId

    @UiThread // via TelemetryHomeTileUniqueClickPerSessionCounter
    fun startSession(context: Context) {
        TelemetryHolder.get().recordSessionStart()
        TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
        // Glean Probe
        App.appForeground.record()

        // We call reset in both startSession and stopSession. We call it here to make sure we
        // clean up before a new session if we crashed before stopSession.
        resetSessionMeasurements(context)
    }

    @UiThread // via TelemetryHomeTileUniqueClickPerSessionCounter
    fun stopSession(context: Context) {
        // We cannot use named arguments here as we are calling into Java code
        TelemetryHolder.get().recordSessionEnd { // onFailure =
            sentryIntegration.capture(IllegalStateException("Telemetry#recordSessionEnd called when no session was active"))
        }

        TelemetryEvent.create(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
        // Glean Probe
        App.appBackground.record()

        // We call reset in both startSession and stopSession. We call it here to make sure we
        // don't persist the user's visited tile history on disk longer than strictly necessary.
        queueSessionMeasurements(context)
        resetSessionMeasurements(context)
    }

    // EXT to add events to pocket ping (independently from mobile_events)
    fun TelemetryEvent.queueInPocketPing() {
        if (!TelemetryHolder.get().configuration.isCollectionEnabled) {
            return
        }

        (TelemetryHolder.get()
                .getPingBuilder(TelemetryPocketEventPingBuilder.TYPE) as TelemetryPocketEventPingBuilder)
                .eventsMeasurement.add(this)
    }

    private fun queueSessionMeasurements(context: Context) {
        TelemetryHomeTileUniqueClickPerSessionCounter.queueEvent(context)
        TelemetryEvent.create(Category.AGGREGATE, Method.CLICK, Object.POCKET_VIDEO,
                pocketUniqueClickedVideoIDs.size.toString()).queue()
        queuePocketVideoImpressionEvent()
        queuePocketVideoClickEvent()
    }

    private fun resetSessionMeasurements(context: Context) {
        TelemetryHomeTileUniqueClickPerSessionCounter.resetSessionData(context)
        TelemetryRemoteControlTracker.resetSessionData(context)
        pocketUniqueClickedVideoIDs.clear()
        pocketUniqueImpressedVideoIDs.clear()
    }

    fun stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryMobileEventPingBuilder.TYPE)
                .queuePing(TelemetryPocketEventPingBuilder.TYPE)
                .scheduleUpload()
    }

    fun urlBarEvent(isUrl: Boolean, autocompleteResult: AutocompleteResult, inputLocation: UrlTextInputLocation) {
        if (isUrl) {
            TelemetryIntegration.INSTANCE.browseEvent(autocompleteResult, inputLocation)
        } else {
            TelemetryIntegration.INSTANCE.searchEnterEvent(inputLocation)
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

        val context = telemetry.configuration.context
        val searchEngine = context.serviceLocator.searchEngineManager.getDefaultSearchEngine(context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

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
        // Glean Probe
        Browser.sslError.record(mapOf(Browser.sslErrorKeys.sslErrorType to primaryErrorMessage))
    }

    fun fullScreenVideoProgrammaticallyClosed() {
        TelemetryEvent.create(Category.ACTION, Method.PROGRAMMATICALLY_CLOSED, Object.FULL_SCREEN_VIDEO).queue()
        // Glean probe
        Video.fullscreenClosed.record()
    }

    @UiThread // via TelemetryHomeTileUniqueClickPerSessionCounter
    fun homeTileClickEvent(context: Context, tile: ChannelTile) {
        if (tile.id == YOUTUBE_TILE_ID) {
            TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE,
                    Value.YOUTUBE_TILE).queue()
            // Glean Probe
            Tiles.youtubeTile.record()
        }
        // Add an extra that contains the tileId for bundled tiles only
        val tileType = getTileTypeAsStringValue(tile)
        if (tileType == Value.TILE_BUNDLED) {
            TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE, tileType)
                .extra(Extra.TILE_ID, tile.id)
                .queue()
            // Glean Probe
            Tiles.homeTile.record(mapOf(Tiles.homeTileKeys.homeTileId to tile.id))
        } else {
            TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOME_TILE, tileType).queue()
            // Glean Probe
            Tiles.homeTile.record(mapOf(Tiles.homeTileKeys.homeTileId to "Not bundled tile"))
        }
        TelemetryHomeTileUniqueClickPerSessionCounter.countTile(context, tile)
    }

    internal fun homeTileUniqueClickCountPerSessionEvent(uniqueClickCountPerSession: Int) {
        TelemetryEvent.create(Category.AGGREGATE, Method.CLICK, Object.HOME_TILE, uniqueClickCountPerSession.toString())
                .queue()
    }

    fun clearDataEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, Value.CLEAR_DATA).queue()
        // Glean Probe
        Setting.clearData.record()
    }

    fun settingsTileClickEvent(tile: SettingsTile) {
        val telemetryValue = when (tile) {
            SettingsScreen.DATA_COLLECTION -> Value.SETTINGS_SEND_DATA_TILE
            SettingsScreen.CLEAR_COOKIES -> Value.SETTINGS_CLEAR_DATA_TILE
            SettingsButton.ABOUT -> Value.SETTINGS_ABOUT_TILE
            SettingsButton.PRIVACY_POLICY -> Value.SETTINGS_PRIVACY_TILE
            else -> null
        }
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.SETTING, telemetryValue).queue()
        // Glean Probe
        Setting.clickSetting.record(mapOf(Setting.clickSettingKeys.settingName to (telemetryValue.toString())))
    }

    // TODO send different values depending on the Fxa state
    fun fxaButtonClickEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.FXA, Value.FXA_LOGIN_BUTTON).queue()
    }

    /**
     * User presses menu button to open overlay (i.e. if menu closes overlay, this isn't counted).
     */
    fun menuOpenedFromMenuButton() {
        // Note: Method.USER_HIDE is no longer used and replaced by NO_ACTION_TAKEN (see telemetry docs).
        TelemetryEvent.create(Category.ACTION, Method.USER_SHOW, Object.MENU).queue()
        // Glean Probe
        Nav.showOverlay.record()
    }

    /**
     * This event is sent when a user opens the menu and then closes it without
     * appearing to find what they are looking for.
     *
     * See [MenuInteractionMonitor] kdoc for more information.
     */
    fun menuUnusedEvent() {
        TelemetryEvent.create(Category.AGGREGATE, Method.NO_ACTION_TAKEN, Object.MENU).queue()
        // Glean Probe
        Nav.unusedEvent.record()
    }

    fun overlayClickEvent(
        event: NavigationEvent,
        isTurboButtonChecked: Boolean,
        isPinButtonChecked: Boolean,
        isDesktopModeButtonChecked: Boolean
    ) {
        val telemetryValue = when (event) {
            NavigationEvent.BACK -> Value.BACK
            NavigationEvent.FORWARD -> Value.FORWARD
            NavigationEvent.RELOAD -> Value.RELOAD
            NavigationEvent.EXIT_FIREFOX -> Value.EXIT_FIREFOX

            // For legacy reasons, turbo has different telemetry params so we special case it.
            // Pin has a similar state change so we model it after turbo.
            NavigationEvent.TURBO -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.TURBO_MODE, boolToOnOff(isTurboButtonChecked)).queue()
                // Glean Probe
                Toggles.turboMode.set(isTurboButtonChecked)
                return
            }
            NavigationEvent.PIN_ACTION -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.PIN_PAGE, boolToOnOff(isPinButtonChecked))
                        .extra(Object.DESKTOP_MODE, boolToOnOff(isDesktopModeButtonChecked))
                        .queue()
                // Glean Probe
                Pin.pinPage.set(isPinButtonChecked)
                return
            }
            NavigationEvent.DESKTOP_MODE -> {
                TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.DESKTOP_MODE,
                        boolToOnOff(isDesktopModeButtonChecked)).queue()
                // Glean Probe
                Toggles.desktopMode.set(isDesktopModeButtonChecked)
                return
            }

            // Settings telemetry handled in a separate event
            NavigationEvent.SETTINGS_DATA_COLLECTION, NavigationEvent.SETTINGS_CLEAR_COOKIES -> return

            // Load is handled in a separate event
            NavigationEvent.LOAD_URL, NavigationEvent.LOAD_TILE -> return

            NavigationEvent.FXA_BUTTON -> return // TODO: #2512 add telemetry for FxA login.
        }
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, telemetryValue).queue()
        // Glean Probe
        Nav.browserMenu.record(mapOf(Nav.browserMenuKeys.browserClickType to telemetryValue))
    }

    /** The browser goes back from a controller press. */
    fun browserBackControllerEvent() {
        TelemetryEvent.create(Category.ACTION, Method.PAGE, Object.BROWSER, Value.BACK)
                .extra(Extra.SOURCE, "controller")
                .queue()
        // Glean Probe
        Nav.browserBackCtr.record(mapOf(Nav.browserBackCtrKeys.sourceOfClick to "controller"))
    }

    fun homeTileRemovedEvent(removedTile: ChannelTile) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.HOME_TILE,
                getTileTypeAsStringValue(removedTile)).queue()
        // Glean Probe
        Tiles.removeTile.record(mapOf(Tiles.removeTileKeys.tileType to getTileTypeAsStringValue(removedTile)))
    }

    @AnyThread // pocketUniqueClickedVideoIDs is synchronized.
    fun pocketVideoClickEvent(id: String) {
        pocketUniqueClickedVideoIDs.add(id)
    }

    @AnyThread // pocketUniqueImpressVideoIDs is synchronized.
    fun pocketVideoImpressionEvent(id: String) {
        pocketUniqueImpressedVideoIDs.add(id)
    }

    private fun queuePocketVideoImpressionEvent() {
        for (videoId in pocketUniqueImpressedVideoIDs) {
            TelemetryEvent.create(Category.POCKET, Method.IMPRESSION, Object.VIDEO_ID,
                    videoId).queueInPocketPing()
            // Glean Probe
            Pocket.pocketImpression.record(mapOf(Pocket.pocketImpressionKeys.pocketImpressionId to videoId))
        }
    }

    private fun queuePocketVideoClickEvent() {
        for (videoId in pocketUniqueClickedVideoIDs) {
            TelemetryEvent.create(Category.POCKET, Method.CLICK, Object.VIDEO_ID,
                    videoId.toString()).queueInPocketPing()
            // Glean Probe
            Pocket.pocketClick.record(mapOf(Pocket.pocketClickKeys.pocketClickId to videoId))
        }
    }

    fun mediaSessionEvent(eventType: MediaSessionEventType) {
        val method = when (eventType) {
            MediaSessionEventType.PLAY_PAUSE_BUTTON -> Method.CLICK
            else -> Method.CLICK_OR_VOICE
        }
        TelemetryEvent.create(Category.ACTION, method, Object.MEDIA_SESSION, eventType.value).queue()
        // Glean Probe
        Video.mediaSessionEvent.record(mapOf(Video.mediaSessionEventKeys.mediaEventType to eventType.value))
    }

    private fun boolToOnOff(boolean: Boolean) = if (boolean) Value.ON else Value.OFF

    private fun getTileTypeAsStringValue(tile: ChannelTile) = when (tile.tileSource) {
        TileSource.BUNDLED -> Value.TILE_BUNDLED
        TileSource.CUSTOM -> Value.TILE_CUSTOM
        TileSource.POCKET -> Value.TILE_POCKET
        TileSource.NEWS -> Value.TILE_BUNDLED
        TileSource.SPORTS -> Value.TILE_BUNDLED
        TileSource.MUSIC -> Value.TILE_BUNDLED
    }

    fun youtubeCastEvent() {
        TelemetryEvent.create(Category.ACTION, Method.YOUTUBE_CAST, Object.BROWSER).queue()
        // Glean Probe
        Browser.youtubeCast.record()
    }

    @UiThread
    fun saveRemoteControlInformation(context: Context, keyEvent: KeyEvent) =
            TelemetryRemoteControlTracker.saveRemoteControlInformation(context, keyEvent)

    fun viewIntentEvent() {
        TelemetryEvent.create(Category.ACTION, Method.VIEW_INTENT, Object.APP).queue()
        // Glean Probe
        App.viewIntent.record()
    }

    fun recordActiveExperiments(experimentNames: List<String>) {
        TelemetryHolder.get().recordActiveExperiments(experimentNames)
    }
}

enum class MediaSessionEventType(internal val value: String) {
    PLAY("play"), PAUSE("pause"),
    NEXT("next"), PREV("prev"),
    SEEK("seek"),

    PLAY_PAUSE_BUTTON("play_pause_btn")
}

enum class UrlTextInputLocation(internal val extra: String) {
    // We hardcode the Strings so we can change the enum
    HOME("home"),
    MENU("menu"),
}

/** Counts the number of unique home tiles per session the user has clicked on. */
@UiThread // We get-and-set over SharedPreferences in countTile so we need resource protection.
private object TelemetryHomeTileUniqueClickPerSessionCounter {

    fun countTile(context: Context, tile: ChannelTile) {
        Assert.isUiThread()
        if (!TelemetryHolder.get().configuration.isCollectionEnabled) { return }

        val sharedPrefs = getSharedPrefs(context)
        val clickedTileIDs = (sharedPrefs.getStringSet(KEY_CLICKED_HOME_TILE_IDS_PER_SESSION, null)
                ?: setOf()).toMutableSet() // create a copy: we can't modify a SharedPref StringSet.
        val wasNewTileAdded = clickedTileIDs.add(tile.id)

        if (wasNewTileAdded) {
            sharedPrefs.edit()
                    .putStringSet(KEY_CLICKED_HOME_TILE_IDS_PER_SESSION, clickedTileIDs)
                    .apply()
        }
    }

    fun queueEvent(context: Context) {
        Assert.isUiThread()

        val uniqueClickCount = getSharedPrefs(context)
                .getStringSet(KEY_CLICKED_HOME_TILE_IDS_PER_SESSION, null)?.size ?: 0
        TelemetryIntegration.INSTANCE.homeTileUniqueClickCountPerSessionEvent(uniqueClickCount)
    }

    fun resetSessionData(context: Context) {
        Assert.isUiThread()
        getSharedPrefs(context).edit()
                .remove(KEY_CLICKED_HOME_TILE_IDS_PER_SESSION)
                .apply()
    }
}

/** Tracks the name of the remote control used. Only the first remote used per session is reported.
 * If the remote name returns as null, we save as "null". This way, we can also determine how many
 * users use remotes that are not recognized. */
@UiThread // We get-and-set over SharedPreferences so we need resource protection.
private object TelemetryRemoteControlTracker {

    fun saveRemoteControlInformation(context: Context, keyEvent: KeyEvent) {
        Assert.isUiThread()
        if (!TelemetryHolder.get().configuration.isCollectionEnabled) { return }

        val remoteName = InputDevice.getDevice(keyEvent.deviceId)?.name ?: "null"
        val sharedPrefs = getSharedPrefs(context)

        if (!sharedPrefs.contains(KEY_REMOTE_CONTROL_NAME)) {
            sharedPrefs.edit()
                    .putString(KEY_REMOTE_CONTROL_NAME, remoteName)
                    .apply()
        }
    }

    fun resetSessionData(context: Context) {
        Assert.isUiThread()
        getSharedPrefs(context).edit()
                .remove(KEY_REMOTE_CONTROL_NAME)
                .apply()
    }
}

private fun getSharedPrefs(context: Context) = context.getSharedPreferences(SHARED_PREFS_KEY, 0)
