# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/).
It diverges in the following ways:
- Release titles do not link to the commits within the release
- This project only strictly adheres to
[Semantic Versioning](http://semver.org/spec/v2.0.0.html) for bug fix releases.

## Unreleased

## 4.7.1 - 2020-07-14
### Fixed
- deletion-request ping is sent correctly whenever the user opts out of data collection (#3013)

## 4.7 - 2020-06-30
### Changed
- Removed Pocket from Homepage (#2988)

## 4.6 - 2019-11-25
### Changed
- After successful Firefox Account sign in, the page the user was on before sign in is restored (#2805)
- Improved behavior of soft keyboard `submit` button on whitelisted sites (#1962)
- FxA account state will be updated when resuming the app (#2806)

### Fixed
- Bundled Pocket titles correctly open YouTube (#2912)

## 4.5 - 2019-09-23
### Added
- Firefox Account sign in and Send Tab is available to release users

### Changed
- Amazon Device Messaging (ADM) is supported in release builds (#2781)
- If a tab is sent while the app is backgrounded, it will be shown the next time FFTV is opened (#2807)

## 4.4.1 - 2019-09-17
### Added
- Dialog prompting users to manually upgrade the app (#2794)

### Fixed
- Toasts not appearing for pinning/unpinning and desktop mode (#2864)

## 4.4 - 2019-09-09
### Added
- (disabled by default) Receive tabs onboarding screen (#2495)
- (disabled by default) FxA reauthentication state icon, text, and telemetry (#2493)

### Changed
- Stripped out code for unsupported architectures (#2647)

## 4.3 - 2019-08-26
### Added
- (disabled by default) Support for Amazon Device Messaging (#2528)
- Experiment for turbo mode rebrand (#2689)

### Fixed
- Fullscreen videos were offset if the page was scrolled down before fullscreening. This fix will not work under certain conditions and on certain sites (#2540. #2541)
- Fixed a rare crash that could occur when Pocket tiles with longer than average text were quickly scrolled to the right (#2727)

## 4.2 - 2019-08-12
### Added
- Tile focus animation and overlay fade animation (#2454)
- (disabled by default) FxA profile screen (#2494)

### Changed
- Refactored out FocusRepo in favor of native focus handling; no user-facing changes are expected (#2395)
- (disabled by default) Use final logo for Receive Tabs toolbar button (#2489)

### Fixed
- Remote Settings URL goes through the CDN (#2651)

## 4.1 - 2019-07-29
### Fixed
- Prevent some videos from being offset to the right, with a black strip on the left. (#2526)

## 4.0 - 2019-07-15
### Added
- QA: command-line control to set current locale (#2418)

### Fixed
- Crash when last tile deleted (#2444)
- Categories remain displayed even if deleting all the videos from each tile section (#2448)
- Stuck after deleting the last pinned tile (#2447)

## 3.11 - 2019-06-25
### Added
- TV Guide Channel Onboarding

### Changed
- Removed outdated Pocket onboarding screen (#2179)

## 3.10 - 2019-06-10
### Added
- (Behind experiment flag, set to 0 at release time) Various channels containing bundled links to various video-friendly websites (#2195)
- QA: command-line control to opt-in or out of experiments (#2320)
- Integrate Glean SDK (#1652)

### Changed
- Move Pocket into a channel (#2111, #2280)
  - Initially displayed Pocket video recs are bundled with the app
  - Pocket data is updated once overnight (#2223), rather than every 45 minutes

### Fixed
- Pocket onboarding was shown to users that don't see Pocket content (#2293)
- Crash when deleting the last pinned tile (#2354)

## 3.9 - 2019-05-13
### Added
- Channel for displaying pinned tiles (replacing the previous container style) (#2110)

### Changed
- Improve cursor performance and feel (#1912)
- Moved Pocket content into a Channel (#2111)
- Updated Pinned Tiles logos and styling for Channels (#2280)


## 3.8 - 2019-04-30
### Added
- Move Settings into channel on the homescreen (#689)

### Fixed
- Fix Pocket timeout exceptions (#2095)

## 3.7.1 - 2019-04-17
*Released to all Firefox TV devices*
### Fixed
- YouTube frozen after opening overlay (#2072)

## 3.7-RO - 2019-04-16
*Released to Fire TV 4K, staged roll-out*
### Added
- Tooltips on toolbar buttons (#1908)
- Hint bar on home and browser screens (#1907)

### Fixed
- Fixed Youtube loading screen stuck issue (#2031)
- Fixed bug that could cause websites to show up as blank, grey screens when resuming Firefox (#1940)

## 3.5-B - 2019-04-16
*Version bump only, no change from v3.5-A. Released to all devices except Fire TV 4K*

## 3.6-RO - 2019-03-26
*Released to Fire TV 4K, staged roll-out*
### Fixed
- Bug that prevented the toolbar back button from working on YouTube (#1927)

## 3.5-A - 2019-03-26
*Staged rollout of 3.5, no change from v3.5-RO. Released to all devices except Fire TV 4K*

## 3.5-RO - 2019-03-12
*Released to Fire TV 4K, staged roll-out*

### Changed
- Updated dependencies to AndroidX

### Fixed
- Bug where you could not use remote back button to exit YouTube if it was visited from Pocket (#1584)
- Fixed 4K YT bug (#277)
- Bug where cursor would disappear if on overlay during page load finish (#1732)
- Bug that could cause a crash on startup (#1778)
- Fixed links on settings About page that did not load (#1731)
- A memory leak (#1628)
- Fixed bug that could cause YouTube to display vertically offset from where it should be (#1719)
- Bug that could cause YouTube to be unresponsive if the overlay was opened during loading (#1830)
- Fixed Youtube back bug (#1939)
- Workaround for "grey screen" when returning to YouTube from Amazon homescreen (#1865)
- Backing from the navigation overlay when on the first site in your backstack will now properly exit the app (#1916)

## 3.4-B - 2019-03-12
*Version bump only, no change from v3.4-A. Released to all devices except Fire TV 4K*

## 3.4-A - 2019-02-12
*Released to all devices*
Rollback to 3.1.3 (same as 3.2.5) due to 4k bug and other regressions. Version bump only.

## 3.3.1 - 2019-02-04
*Released to Fire TV 4K.*

### Added
- Button to homescreen to exit Firefox

### Fixed
- Fixed Pocket Crash issue for non-EN locales (#1684)
- Crash when using unrecognized remote control (#1685)
- Fixed mismatch between internal and display state after clearing data (#1691)

## 3.3 - 2019-02-04
*Released to all devices except Fire TV 4K.*

Version-bump only - no change from v3.2.5

## 3.2.5 - 2019-01-16
*Rollback to v3.1.3*

Several crashes were found in the previously released version, so this was rolled back to the most recent stable release (v3.1.3).

## 3.2.3 and 3.2.4 - **never released**

These version numbers were skipped due to build errors. They had to be re-built with a bumped version.

## 3.2.2 - 2019-01-15
### Fixed 
- Accessibility on Pocket megatile

## 3.2.1 - 2019-01-14
### Fixed
- Fixed a bug where "send usage data" could not be unchecked

## 3.2 - 2019-01-11
### Added
- Clicking remote menu button from Pocket feed now returns to overlay

### Fixed
- Pocket requests are no longer made if a valid key is not found
- Pocket requests are no longer made when the locale is not set to English

## 3.1.3 - 2018-12-20
### Added
- Ability to exit Firefox with remote back button

### Fixed
- Issue where users were unable to exit YouTube with remote back button (#1542)

## 3.1.2 - 2018-12-11
### Changed
- Removed limited-time event tile from homescreen

## 3.1.1 - 2018-12-07
### Added
- Added limited-time event tile to homescreen

## 3.1 - 2018-11-16
### Added
- Ability to request desktop version of the current site

### Changed
- Hide Pocket video tile on non-EN locales
    - Pocket does not yet provide content for other languages
- Sentry crash reports include a UUID to distinguish users so we can determine if it's 1 user crashing 100 times or 100 users crashing 1 time each. This identifier is only used for Sentry and can not be correlated with telemetry interaction data. See [fire TV Sentry docs](https://github.com/mozilla-mobile/firefox-tv/wiki/Crash-reporting-with-Sentry) for more details. (#817)
- Pocket videos now display the author of the video instead of the platform

### Fixed
- Blank screen when pressing back from a full-screened video
- Infrequent crash caused by initialization logic (#1159)
- On a fresh install, focus would once skip past the Pocket megatile
- A crash that could occur when backing out of the Pocket screen
- DRM content not playing: this is now supported for some DRM videos
- Fixed bug where clearing data would not clear state from the current session
- Some web fonts were blocked which caused icons to be missing

## 3.0.2 - 2018-10-30
*Version-bump only: Released v3.0+ for the first time to Stick Gen 1 & 2 in addition to Fire TV (Gen 1, 2, 3), Cube, Element 4k (pendant), which already had v3.0+.*

## 3.0.1 - RC candidate, did not release
### Fixed
- Issue that would cause browsing history to be lost when navigating to device home screen (#1256)

## 3.0 - 2018-10-16
*Released to Fire TV Cube and Fire TV 4K.*

### Added
- Pocket video recommendations on navigation overlay

### Changed
- UI improvements
    - Merge home screen into navigation overlay
- Long-click and hold to remove home tile instead of pressing the menu button

### Fixed
- Startup time improvements

## 2.2 - 2018-08-21
*Released to Fire TV Cube and Fire TV 4K.*

### Added
- Voice commands to control video playback state: "Alexa
play/pause/rewind/fast-forward/restart/next/previous"

### Changed
- Require Amazon voice control permission: `com.amazon.permission.media.session.voicecommandcontrol`

### Fixed
- Media will autoplay, preventing black screens between videos on YouTube (#586)

## Note on releases below
The CHANGELOG entries for the releases listed below this were added retroactively and may be incomplete.

## 2.1.2 2018-10-30
*Version-bump only for Fire TV Stick 4k*

## 2.1 - 2018-?-?
### Added
- Crash reporting with Sentry (see project docs for more information; #429)

### Changed
- Require `android.permission.ACCESS_NETWORK_STATE` for Sentry crash reporting
- Improve Your Rights page

### Fixed
- Made cursor behavior smoother (#472)
- Various performance improvements

## 2.0.1 - 2018-?-?
### Fixed
- Top crasher that doesn't seem to require any specific user interaction (#694)

## 2.0 - 2018-?-?
### Added
- Ability to pin sites to the Firefox home screen
- Ability to remove sites from the Firefox home screen

### Changed
- Improve navigation controls for web browsing

## 1.1.2 - 2018-02-?
### Fixed
- Crash when leaving the app when video is fullscreened
- Various German translations

## 1.1.1 - 2018-02-?
### Changed
- A URL to ensure users see the best formatted website

## 1.1 - 2018-01-?
### Added
- Turbo mode
- Better support for VoiceView screen reader features

## 1.0.1 - 2017-12-?
### Fixed
- Icon artifacts on older versions of Android
- Various stability issues

## 1.0 - 2017-12-20
*Initial release! A browser including home tile shortcuts.*
