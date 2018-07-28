# Firefox for Amazon's Fire TV

[![Task Status](https://github.taskcluster.net/v1/repository/mozilla-mobile/firefox-tv/master/badge.svg)](https://github.taskcluster.net/v1/repository/mozilla-mobile/firefox-tv/master/latest)
[![codecov](https://codecov.io/gh/mozilla-mobile/firefox-tv/branch/master/graph/badge.svg)](https://codecov.io/gh/mozilla-mobile/firefox-tv)

_Fast for good, just right for watching video at home. A browser for
discovering and watching web video on the big screen TV for users to install on
their Amazon Fire TV and Fire TV stick._

[Get it on Amazon Fire TV][amazon link]

## Getting Involved
Our code is open source and we encourage all positive contributions! We love pull
requests, bug reports, ideas, (security) code reviews and other kinds of contributions.
Before you contribute, please read the [Community Participation
Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* [Guide to Contributing][contribute] (**new contributors start here!**)
* Open issues: https://github.com/mozilla-mobile/firefox-tv/issues
* Project wiki: https://github.com/mozilla-mobile/firefox-tv/wiki
* IRC: [#focus (irc.mozilla.org)](https://wiki.mozilla.org/IRC) + logs:
https://mozilla.logbot.info/focus/; we're available Monday-Friday, GMT and PST
working hours.
* Mailing list:
[firefox-focus-public@](https://mail.mozilla.org/listinfo/firefox-focus-public)

**Beginners!** Watch out for [Issues with the `good first issue` label](https://github.com/mozilla-mobile/firefox-tv/labels/good%20first%20issue).
These are easy bugs that have been left for first timers to have a go, get involved and make a
positive contribution to the project!

For other contributor-ready issues, see [the `help wanted` label](https://github.com/mozilla-mobile/firefox-tv/labels/help%20wanted).

## Build instructions
1. Clone the repository:

  ```shell
  git clone https://github.com/mozilla-mobile/firefox-tv
  ```

2. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleAmazonWebviewDebug
  ```

3. Make sure to select the right build variant in Android Studio: **amazonWebviewDebug**

### Running
It is recommended to test directly on a Fire TV: see the [developer guide][dev guide] for more info.
You can connect with:
```shell
adb connect <IP address>
```

And then install via Android Studio or adb. Only a single development device
can be connected to a Fire TV at a time. Note that while you can install on an
Android TV emulator, the behavior is different from Fire TV's and should not be
relied upon.

When using an emulator, you can press `cmd+m` to simulate a menu button press.

### Building with API keys
Certain services require an API key, so you'll need to build with the key to use them in the apk.

1. To build with the API key (for services such as Sentry), add a `<project-dir>/.<service>_debug`
file with your key, for example, `<project-dir>/.sentry_dsn_debug`

    1. To enable Sentry on Debug builds, additionally replace the `DataUploadPreference.isEnabled`
value with true (upload is disabled by default in dev builds).

2. Verify the key add was successful. The gradle output is the only way to verify this (although
it won't indicate if the key is valid). You will see a message in the gradle output
indicating the key was added:

`Sentry DSN (amazonWebviewDebug): Added from /Users/mcomella/dev/moz/firefox-tv/.sentry_dsn_debug`

As opposed to:

`Sentry DSN (amazonWebviewDebug): X_X`

API services currently supported are:
* sentry_dsn
* pocket_key

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

[amazon link]: https://www.amazon.com/dp/B078B5YMPD/ref=sr_1_1
[dev guide]: https://github.com/mozilla-mobile/firefox-tv/wiki/Developer-guide-and-differences-from-Android
[contribute]: https://github.com/mozilla-mobile/shared-docs/blob/master/android/CONTRIBUTING.md
