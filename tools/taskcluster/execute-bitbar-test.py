# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script initiates a UI test-run after a upload of both the application and test
APKs to Bitbar Cloud.
"""

from testdroid import Testdroid
import json
import os
import sys
import time

token_file_path = os.path.join(os.path.dirname(__file__), '../../.bitbar_token.json')
with open(token_file_path, 'r') as json_file:
    token_data = json.load(json_file)

testdroid = Testdroid(apikey=token_data['api_key'], url=token_data['cloud_url'])

if len(sys.argv) != 3:
    print "Bitbar Cloud: Application and Test Application APK path required."
    exit(1)

app_apk = testdroid.upload_file(os.path.join(os.path.dirname(__file__), "../../app/build/outputs/apk/%s/%s.apk" % (sys.argv[1], sys.argv[2]))) # Application
test_app_apk = testdroid.upload_file(os.path.join(os.path.dirname(__file__), "../../app/build/outputs/apk/androidTest/%s/%s-androidTest.apk" % (sys.argv[1], sys.argv[2]))) # Test Application

# Custom configuration using preset: android framework, project & device group
test_run = testdroid.start_test_run_using_config(json.dumps({
    "deviceGroupId": 42,
    "files": [{"id": app_apk['id']}, {"id": test_app_apk['id']}],
    "frameworkId": 24,
    "limitationType": "CLASS",
    "limitationValue": "org.mozilla.tv.firefox.ui",
    "osType": "ANDROID",
    "projectId": 296208,
    "scheduler": "PARALLEL",
    "testRunName": str(time.strftime("%m/%d/%Y %H:%M:%S"))
}))

print "Bitbar Cloud: UI Test Run Created - " + str(test_run['id'])

# Automatically polls for completion of a test run
testdroid.wait_test_run(test_run['projectId'], test_run['id'])

# Check for test run failures and exit on test failure
test_result = testdroid.get_test_run(test_run['projectId'], test_run['id'])

if test_result['failedTestCaseCount'] != 0:
    print "Bitbar Cloud: %d UI Test(s) have failed (Run ID: %s). Please check #bitbar-firefox-tv in Slack for URL." % (test_result['failedTestCaseCount'], test_result['displayName'])
    exit(1)
else:
    print "Bitbar Cloud: UI Test(s) have passed (Run ID: %s)!" % (test_run['displayName'])
