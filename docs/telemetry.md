# Telemetry
For clients that have "send anonymous usage data" enabled Firefox for Fire TV sends a "core" ping and an "event" ping to Mozilla's telemetry service. Sending telemetry can be disabled in the app's settings. Builds of "Firefox for Fire TV" have telemetry enabled by default ("opt-out").

# Core ping

Firefox for Fire TV creates and tries to send a "core" ping whenever the app goes to the background. This core ping uses the same format as Firefox for Android and is [documented on firefox-source-docs.mozilla.org](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html).

# Event ping

In addition to the core ping an event ping for UI telemetry is generated and sent as soon as the app is sent to the background.

## Settings

As part of the event ping the most recent state of the user's setting is sent (default values in **bold**):

| Setting                  | Key                             | Value
|--------------------------|---------------------------------|----------------------
| Turbo mode currently enabled?      | tracking_protection_enabled     | **true**/false
| Total home tile count    | total_home_tile_count           | `<int>`
| Custom home tile count   | custom_home_tile_count          | `<int>`
| App ID                   | app_id                          | `<string>`*

(*) This is the public, published app ID, and does not contain any user information


## Events

The event ping contains a list of events ([see event format on readthedocs.io](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html)) for the following actions:

### Sessions

| Event                                    | category | method     | object | value  |
|------------------------------------------|----------|------------|--------|--------|
| Start session (App is in the foreground) | action   | foreground | app    |        |
| Stop session (App is in the background)  | action   | background | app    |        |

### General

| Event                                  | category | method                | object     | value  | extras.    |
|----------------------------------------|----------|-----------------------|------------|--------|------------|
| Settings: confirms clear data dialog   | action   | change                | setting    | clear_data | |
| Pocket video feed: unique videos clicked per session|aggregate|click      | pocket_video | `<int>` | |

(*) This event is sent at the end of every session.

### Browser Overlay
| Event                                  | category | method                | object     | value  | extras.    |
|----------------------------------------|----------|-----------------------|------------|--------|------------|
| Settings clicked                       | action   | click                 | menu       | settings | |
| Browser: back clicked                  | action   | click                 | menu       | back   | |
| Browser: forward clicked               | action   | click                 | menu       | forward | |
| Browser: refresh clicked               | action   | click                 | menu       | refresh | |
| Pocket: tile clicked                   | action   | click                 | menu       | pocket_video_tile | |
| Turbo mode switch clicked              | action   | change                | turbo_mode | on/off (the new value) | |
| Pin site switch clicked                | action   | change                | pin_page   | on/off (the new value) | desktop_mode* |
| Desktop mode switch clicked | action | change | desktop_mode | on/off (the new value) | |
| Tile clicked                           | action   | click                 | home_tile  | bundled/custom | |
| Tile removed                           | action   | remove                | home_tile  | bundled/custom | |
| Unique tiles clicked per session**     | aggregate| click                 | home_tile  | `<int>` | |
| Menu shown by user              | action   | user_show       | menu         | ||
| Menu hidden by user              | action   | user_hide       | menu         | ||
| No menu action taken              | aggregate   | no_action_taken       | menu         | ||

(*)When the pin site switch is clicked, the state (on/off) of the desktop mode switch is also sent.

(**)This is broken: see [#879](https://github.com/mozilla-mobile/firefox-tv/issues/879).** For overlay hidden, we only capture cases where the user closes the overlay themselves:
- Controller menu button is pressed
- Controller back button is pressed

e.g. we don't log an event when the overlay is closed automatically after "Home" is selected.

### Browsing

| Event                                  | category | method                | object     | value  | extras.    |
|----------------------------------------|----------|-----------------------|------------|--------|------------|
| URL start loading (via url bar)        | action   | type_url              | search_bar |        | `url`*     |
| Search query start loading (via url bar)| action  | type_query            | search_bar |        | `query`*   |
| Remote: back pressed **                | action   | page                  | browser    | back   | `back`*    |
| Video opened from YouTube Casting | action | youtube_cast | browser |  |  |

(*) `query` is a JSON map containing the url bar location:
```js
{
  "source": "home"/"menu"
}
```

(*) `url` is a JSON map containing the same map as `query` and the autocomplete details:
```js
{
  // Includes "source" above

  "autocomplete": true,         // Was the URL autocompleted?
  "autocompl_src":  "default",  // Which autocomplete list was used ("default" or "custom")?  (Only present if autocomplete is true)
  "total": 25                   // Total number of items in the used autocomplete list (Only present if autocomplete is true)
}
```

(**) This event fires only when the browser travels back in history, i.e. this event will not fire if:
- The browser is on the last page in history and back is pressed, returning to home/settings or closing the app
- Back is pressed on youtube.com/tv, where we override a back with ESC in order to exit fullscreen mode

(*) `back` is a JSON map containing a hard-coded "controller" string:
```javascript
{
  "source": "controller"
}
```

#### Media playback
| Event                                      | category | method         | object        | value   |
|--------------------------------------------|----------|----------------|---------------|---------|
| MediaSession: play                         | action   | click_or_voice | media_session | play |
| MediaSession: pause                        | action   | click_or_voice | media_session | pause |
| MediaSession: next item                    | action   | click_or_voice | media_session | next |
| MediaSession: previous item                | action   | click_or_voice | media_session | prev |
| MediaSession: seek                         | action   | click_or_voice | media_session | seek |
| MediaSession: play/pause remote button     | action   | click          | media_session | play_pause_btn |

Fire OS controls media (e.g. videos, audio) playback with a `MediaSession`. MediaSession probes are sent when the user interacts with media through Fire OS (e.g. hardware media buttons, voice commands) **but not** when they interact through web content (e.g. pressing pause with the dpad Cursor). Right now, MediaSession **only supports video** ([#935](https://github.com/mozilla-mobile/firefox-tv/issues/935) is to add audio support).

MediaSession doesn't distinguish between hardware buttons and voice commands. If you want to calculate how often voice commands are used for a given command, you should subtract the number of hardware buttons used for a command (if applicable) from the total number of invocations for that command, e.g.: `play_pause_voice_commands = play + pause - play_pause_btn`. The only media button that interacts with MediaSession on a standard Fire TV remote is the play/pause button. 

To elaborate on these events:
- "Next/previous item" is intended to go to the next video/song in a playlist. We send the corresponding key event to the page which must support this functionality (it works on YouTube).
- "Seek" aggregates the "fast-forward", "rewind", and "restart" commands ([#988](https://github.com/mozilla-mobile/firefox-tv/issues/988) is to split up this telemetry)

## Limits

* An event ping will contain up to but no more than 500 events
* No more than 40 pings per type (core/event) are stored on disk for upload at a later time
* No more than 100 pings are sent per day

# Implementation notes

* Event pings are generated (and stored on disk) whenever the onStop() callback of the main activity is triggered. This happens whenever the main screen of the app is no longer visible (The app is in the background or another screen is displayed on top of the app).

* Whenever we are storing pings we are also scheduling an upload. We are using Android’s JobScheduler API for that. This allows the system to run the background task whenever it is convenient and certain criterias are met. The only criteria we are specifying is that we require an active network connection. In most cases this job is executed immediately after the app is in the background.

* Whenever an upload fails we are scheduling a retry. The first retry will happen after 30 seconds (or later if there’s no active network connection at this time). For further retries a exponential backoff policy is used: [30 seconds] * 2 ^ (num_failures - 1)

* An earlier retry of the upload can happen whenever the app is coming to the foreground and sent to the background again (the previous scheduled job is reset and we are starting all over again).
