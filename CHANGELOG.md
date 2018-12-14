# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Ability to exit Firefox with remote back button

### Changed

### Fixed
- Some web fonts were blocked which caused icons to be missing

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

[Unreleased]: https://github.com/mozilla-mobile/firefox-tv/compare/v3.1..HEAD
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
