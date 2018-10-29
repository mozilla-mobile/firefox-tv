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
which zipalign || { echo "Add zipalign to your path" && exit 1; }
which apksigner || { echo "Add apksigner to your path" && exit 1; }

# Do some argument sanity checks.
if (( $# != 2 )); then
  echo "Did you forget to include the keystore and apk as args?"
  exit 1
else
  [[ $1 =~ .*\.apk$ ]] || { echo "Please supply an apk" && exit 1; }
fi

# Check that keystore file actually exists
find $2 || { echo "Do you need to create a keystore file?
To generate a keystore, you can use the following command:
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias" && exit 1; }

# Zipalign and sign
zipalign -v -p 4 $1
apksigner sign --ks $2 --out $1 $1

echo "Created signed apk $1"
