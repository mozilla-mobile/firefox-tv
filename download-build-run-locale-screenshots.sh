#!/bin/bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This script will clone, build, and install Firefox TV to device and 
# then execute screenshot tests which may be uploaded to Dropbox by choice 

# Requirements: Android SDK (adb, gradlew)
#               git
#               fastlane – 'sudo gem install fastlane'
#               sips - scriptable image processing system
#               dropbox client (optional)

# NOTE: Screengrabfile must target a specific device using:
#       specific_device '#####'

REPO="https://github.com/mozilla-mobile/firefox-tv.git"
CLONE_DIR="$HOME/Desktop/firefox-tv"
SCREENSHOTS_DIR="$CLONE_DIR/fastlane"
CONFIG="$CLONE_DIR/Screengrabfile"

# optional – location used to upload screenshots
DROPBOX_DIR="$HOME/Dropbox"

# export target Amazon Fire TV serial (AFTT 5.1.1)
# match your device serial by specifying the device ID
export ANDROID_SERIAL=""

# set Android SDK location
export ANDROID_HOME=$HOME/Library/android/sdk

# wake device and unlock
adb -s $ANDROID_SERIAL shell input keyevent 82

# disable system animations
adb -s $ANDROID_SERIAL shell settings put global window_animation_scale 0
adb -s $ANDROID_SERIAL shell settings put global transition_animation_scale 0
adb -s $ANDROID_SERIAL shell settings put global animatior_duration_scale 0

# clone repository
if [ -d "$CLONE_DIR" ]; then
 cd "$CLONE_DIR"
 git pull origin master
else
 git clone "$REPO" "$CLONE_DIR"
 cd "$CLONE_DIR"
fi

# build debug app
./gradlew clean app:assembleDebug

# build test app
./gradlew app:assembleAndroidTest

# uninstall existing apps
./gradlew uninstallDebug uninstallDebugAndroidTest

# install apps
./gradlew installDebug installDebugAndroidTest

# inject target device into Screengrabfile
echo "specific_device '$ANDROID_SERIAL'" >> $CONFIG

# run fastlane screenshots
fastlane screengrab run

# compress images and rename report
cd "$SCREENSHOTS_DIR"
find . -name "*.png" | xargs sips -Z 640

cd "$SCREENSHOTS_DIR/metadata/android"
find . -name "*.html" -exec mv {} "index.html" \;

# optional upload to Dropbox, uncomment to use
#cp -rf "$SCREENSHOTS_DIR/metadata/android" "$DROPBOX_DIR/android"
