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
  * [`good first issues`][good first] | [`help wanted`][help]
  * [File a security issue][sec issue]
* Project wiki: https://github.com/mozilla-mobile/firefox-tv/wiki
* IRC: [#focus (irc.mozilla.org)](https://wiki.mozilla.org/IRC) | [view logs](https://mozilla.logbot.info/focus/);
we're available Monday-Friday, GMT and PST working hours.
* Mailing list:
[firefox-focus-public@](https://mail.mozilla.org/listinfo/firefox-focus-public)


## Build instructions
1. Clone the repository:

  ```shell
  git clone https://github.com/mozilla-mobile/firefox-tv
  ```

2. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembledebug
  ```

3. Make sure to select the right build variant in Android Studio: **debug**

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

If using an emulator, we recommend the Android TV device image: either 720p or
1080p is fine. You can press `cmd+m` to simulate a menu button press.

### Pre-push hooks
To reduce review turn-around time, we'd like all pushes to run tests locally. We'd
recommend you use our provided pre-push hook in `quality/pre-push-recommended.sh`.
Using this hook will guarantee your hook gets updated as the repository changes.
This hook tries to run as much as possible without taking too much time.

To add it, run this command from the project root:
```sh
ln -s ../../quality/pre-push-recommended.sh .git/hooks/pre-push
```

To push without running the pre-push hook (e.g. doc updates):
```sh
git push <remote> --no-verify
```

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
[good first]: https://github.com/mozilla-mobile/firefox-tv/labels/good%20first%20issue
[help]: https://github.com/mozilla-mobile/firefox-tv/labels/help%20wanted
[sec issue]: https://bugzilla.mozilla.org/enter_bug.cgi?assigned_to=nobody%40mozilla.org&bug_file_loc=http%3A%2F%2F&bug_ignored=0&bug_severity=normal&bug_status=NEW&cf_fx_iteration=---&cf_fx_points=---&component=Security%3A%20General&contenttypemethod=autodetect&contenttypeselection=text%2Fplain&defined_groups=1&flag_type-4=X&flag_type-607=X&flag_type-791=X&flag_type-800=X&flag_type-803=X&form_name=enter_bug&groups=firefox-core-security&maketemplate=Remember%20values%20as%20bookmarkable%20template&op_sys=Unspecified&priority=--&product=Firefox%20for%20FireTV&rep_platform=Unspecified&target_milestone=---&version=unspecified
