#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# If a command fails then do not proceed and fail this script too.
set -ex

# Get token for uploading to codecov
python tools/taskcluster/get-codecov-token.py

# Execute codecov script for uploading report
bash <(curl -s https://codecov.io/bash) -t @.codecov_token
