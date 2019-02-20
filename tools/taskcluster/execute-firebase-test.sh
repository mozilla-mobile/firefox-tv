#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This script installs google cloud sdk, logs into google firebase, executes tests,
# and collects test artifacts into the test_artifacts folder

# If a command fails then do not proceed and fail this script too.
set -e
#########################
# The command line help #
#########################
display_help() {
    echo "Usage: $0 Build_Variant Apk_Name Device_Config [Device_Config...]"
    echo
    echo "Do not put .apk prefix in Apk_Name"
    echo "Device_Config should be in landscape mode (e.g. model=taimen,version=26,orientation=landscape)"
    echo "Example: execute-firebase-test.sh debug app-debug model=taimen,version=26,orientation=landscape"
    echo
}

# Basic parameter check
if [[ $# -lt 3 ]]; then
    echo "Your command line contains $# arguments"
    display_help
    exit 1
fi

# Get test configuration
config_count=`expr $# - 2`
config_param="--device $3"
for ((i=4; i <= $#; i++))
{
    config_param+=" --device ${!i}"
}

# From now on disable exiting on error. If the tests fail we want to continue
# and try to download the artifacts. We will exit with the actual error code later.
set +e

# Execute test set
output=$(./google-cloud-sdk/bin/gcloud --format="json" firebase test android run \
--type instrumentation \
--app ./app/build/outputs/apk/$1/$2.apk \
--test ./app/build/outputs/apk/androidTest/$1/$2-androidTest.apk \
--use-orchestrator \
--environment-variables clearPackageData=true \
--results-bucket firefox-tv_test_artifacts \
--timeout 30m \
--no-auto-google-login $config_param)

exitcode=$?

# Disabling the download of artifacts, they can be found in the Firebase UI URL shown below
#echo "Downloading artifacts"
#mkdir test_artifacts
#./google-cloud-sdk/bin/gsutil ls gs://firefox-tv_test_artifacts | tail -1 | ./google-cloud-sdk/bin/gsutil cp -r -I ./test_artifacts > /dev/null 2>&1

# Now exit the script with the exit code from the test run. (Only 0 if all test executions passed)
if [[ $exitcode -ne 0 ]]; then
    echo "UI Test(s) have failed, please check above URL"
    else
    echo "All UI Test(s) have passed!"
fi

exit $exitcode
