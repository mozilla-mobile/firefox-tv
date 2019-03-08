# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added

### Changed

### Fixed
- Bug where you could not use remote back button to exit YouTube if it was visited from Pocket (#1584)
- Fixed 4K YT bug (#277)
- Backing from the navigation overlay when on the first site in your backstack will now properly exit the app (#1916)

## [3.5-RO] 2019-03-12
*Released to Fire TV 4K, staged roll-out*
### Added

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

## [3.4-B] - 2019-03-12
*Version bump only, no change from v3.4-A. Released to all devices except Fire TV 4K*

## [3.4-A] - 2019-02-12
*Released to all devices*
Rollback to 3.1.3 (same as 3.2.5) due to 4k bug and other regressions. Version bump only.

## [3.3.1] - 2019-02-04
*Released to Fire TV 4K.*

### Added
- Button to homescreen to exit Firefox

### Fixed
- Fixed Pocket Crash issue for non-EN locales (#1684)
- Crash when using unrecognized remote control (#1685)
- Fixed mismatch between internal and display state after clearing data (#1691)

## [3.3] - 2019-02-04
*Released to all devices except Fire TV 4K.*

Version-bump only - no change from v3.2.5

## [3.2.5] - 2019-01-16
*Rollback to v3.1.3*

Several crashes were found in the previously released version, so this was rolled back to the most recent stable release (v3.1.3).

## 3.2.3 and 3.2.4 - **never released**

These version numbers were skipped due to build errors. They had to be re-built with a bumped version.

## [3.2.2] - 2019-01-15
### Fixed 
- Accessibility on Pocket megatile

## [3.2.1] - 2019-01-14
### Fixed
- Fixed a bug where "send usage data" could not be unchecked

## [3.2] - 2019-01-11
### Added
- Clicking remote menu button from Pocket feed now returns to overlay

### Fixed
- Pocket requests are no longer made if a valid key is not found
- Pocket requests are no longer made when the locale is not set to English

## [3.1.3] - 2018-12-20
### Added
- Ability to exit Firefox with remote back button

### Fixed
- Issue where users were unable to exit YouTube with remote back button (#1542)

## [3.1.2] - 2018-12-11
### Changed
- Removed limited-time event tile from homescreen

## [3.1.1] - 2018-12-07
### Added
- Added limited-time event tile to homescreen

## [3.1] - 2018-11-16
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

## [3.0.2] - 2018-10-30
*Version-bump only: Released v3.0+ for the first time to Stick Gen 1 & 2 in addition to Fire TV (Gen 1, 2, 3), Cube, Element 4k (pendant), which already had v3.0+.*

## [3.0.1] - RC candidate, did not release
### Fixed
- Issue that would cause browsing history to be lost when navigating to device home screen (#1256)

## [3.0] - 2018-10-16
*Released to Fire TV Cube and Fire TV 4K.*

### Added
- Pocket video recommendations on navigation overlay

### Changed
- UI improvements
    - Merge home screen into navigation overlay
- Long-click and hold to remove home tile instead of pressing the menu button

### Fixed
- Startup time improvements

## [2.2] - 2018-08-21
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

## [2.1.2] 2018-10-30
*Version-bump only for Fire TV Stick 4k*

## [2.1] - 2018-?-?
### Added
- Crash reporting with Sentry (see project docs for more information; #429)

### Changed
- Require `android.permission.ACCESS_NETWORK_STATE` for Sentry crash reporting
- Improve Your Rights page

### Fixed
- Made cursor behavior smoother (#472)
- Various performance improvements

## [2.0.1] - 2018-?-?
### Fixed
- Top crasher that doesn't seem to require any specific user interaction (#694)

## [2.0] - 2018-?-?
### Added
- Ability to pin sites to the Firefox home screen
- Ability to remove sites from the Firefox home screen

### Changed
- Improve navigation controls for web browsing

## [1.1.2] - 2018-02-?
### Fixed
- Crash when leaving the app when video is fullscreened
- Various German translations

## [1.1.1] - 2018-02-?
### Changed
- A URL to ensure users see the best formatted website

## [1.1] - 2018-01-?
### Added
- Turbo mode
- Better support for VoiceView screen reader features

## [1.0.1] - 2017-12-?
### Fixed
- Icon artifacts on older versions of Android
- Various stability issues

## [1.0] - 2017-12-20
*Initial release! A browser including home tile shortcuts.*

[Unreleased]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.5-RO...HEAD
[3.5-RO]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.4-A...v3.5-RO
[3.4-A]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.3.1...v3.4-A
[3.3.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.3...v3.3.1
[3.3]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.2.5...v3.3
[3.2.5]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.2.2...v3.2.5
[3.2.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.2.1...v3.2.2
[3.2.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.2...v3.2.1
[3.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.1.3...v3.2
[3.1.3]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.1.2...v3.1.3
[3.1.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.1.1...v3.1.2
[3.1.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.1...v3.1.1
[3.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.0.2..v3.1
[3.0.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.0...v3.0.1
[3.0]: https://github.com/mozilla-mobile/firefox-tv/compare/v2.2...v3.0
[2.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v2.1...v2.2
[2.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v2.0.1...v2.1
[2.0.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v2.0...v2.0.1
[2.0]: https://github.com/mozilla-mobile/firefox-tv/compare/v1.1.2...v2.0
[1.1.2]: https://github.com/mozilla-mobile/firefox-tv/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v1.1...v1.1.1
[1.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v1.0.1...v1.1
[1.0.1]: https://github.com/mozilla-mobile/firefox-tv/compare/v1.0-RC1...v1.0.1
[1.0]: https://github.com/mozilla-mobile/firefox-tv/compare/a220db99ea9bd3c05d3750d9c52c3a2d7356698d...v1.0-RC1
