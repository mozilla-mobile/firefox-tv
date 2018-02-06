# Firefox for Amazon's Fire TV

[![BuddyBuild](https://dashboard.buddybuild.com/api/statusImage?appID=5a3050c4aa895000017cd42e&branch=master&build=latest)](https://dashboard.buddybuild.com/apps/5a3050c4aa895000017cd42e/build/latest?branch=master)
[![Task Status](https://github.taskcluster.net/v1/repository/mozilla-mobile/firefox-tv/master/badge.svg)](https://github.taskcluster.net/v1/repository/mozilla-mobile/firefox-tv/master/latest)

_Fast for good, just right for watching video at home. A browser for
discovering and watching web video on the big screen TV for users to install on
their Amazon Fire TV and Fire TV stick._

[Get it on Amazon Fire TV.][amazon link]

## Getting Involved
We encourage you to participate in this open source project. We love Pull
Requests, Bug Reports, ideas, (security) code reviews or any kind of positive
contribution. Please read the [Community Participation
Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* IRC: [#focus (irc.mozilla.org)](https://wiki.mozilla.org/IRC); logs:
https://mozilla.logbot.info/focus/; we're available Monday-Friday, GMT and PST
working hours.

* Mailing list:
[firefox-focus-public@](https://mail.mozilla.org/listinfo/firefox-focus-public)

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
It is recommended to test directly on a Fire TV. You can connect with:
```shell
adb connect <IP address>
```

And then install via Android Studio or adb. Only a single development device
can be connected to a Fire TV at a time. Note that while you can install on an
Android TV emulator, the behavior is different from Fire TV's and should not be
relied upon.

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

[amazon link]: https://www.amazon.com/dp/B078B5YMPD/ref=sr_1_1
