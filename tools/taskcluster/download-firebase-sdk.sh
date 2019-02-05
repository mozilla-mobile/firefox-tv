#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This script installs google cloud sdk, logs into google firebase, executes tests,
# and collects test artifacts into the test_artifacts folder

# If a command fails then do not proceed and fail this script too.

# Install Google Cloud SDK for using Firebase Device Lab
curl https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-233.0.0-linux-x86_64.tar.gz --output ./gcloud.tar.gz
tar -xvf ./gcloud.tar.gz -C .
./google-cloud-sdk/install.sh --quiet
./google-cloud-sdk/bin/gcloud --quiet components update
