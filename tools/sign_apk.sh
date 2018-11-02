#!/bin/bash

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Signs an apk with a local keystore. This script takes an apk and a keystore path as an argument.
#
# To run:
# sh tools/sign_apk.sh <unsigned.apk> <keystore.jks>
#

# Check for signing tools
which zipalign || { echo "Add Android SDK build-tools to your path to use zipalign" && exit 1; }
which apksigner || { echo "Add Android SDK build-tools to your path to use apksigner" && exit 1; }

# Do some argument sanity checks.
if (( $# != 2 )); then
  echo "Missing keystore or apk, usage: ./tools/sign_apk.sh <unsigned.apk> <keystore.jks>"
  exit 1
else
  [[ $1 =~ .*\.apk$ ]] || { echo "Please supply an apk" && exit 1; }
fi

# Check that keystore file actually exists
if [ ! -f $2 ]; then
  echo "Do you need to create a keystore file?\n
  To generate a keystore, you can use the keytool command, see https://developer.android.com/studio/publish/app-signing#signing-manually"
  exit 1
fi

# Zipalign and sign
zout=$(dirname $1)/zipaligned.apk
zipalign -v -p 4 $1 $zout
output=$(dirname $zout)/app-signed.apk
apksigner sign --ks $2 --out $output $zout
rm $zout

echo "Created signed apk $output"
