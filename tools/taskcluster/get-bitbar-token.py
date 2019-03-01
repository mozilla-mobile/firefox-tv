# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script talks to the taskcluster secrets service to obtain the
Bitbar token and write it to the .bitbar_token file in the root
directory.
"""

import os
import taskcluster
import json

# Get JSON data from taskcluster secrets service
secrets = taskcluster.Secrets({'baseUrl': 'http://taskcluster/secrets/v1'})
data = secrets.get('project/mobile/firefox-tv/tokens')

with open(os.path.join(os.path.dirname(__file__), '../../.bitbar_token.json'), 'w') as file:
    json.dump(data['secret']['bitbarToken'], file)

print("Imported Bitbar token from secrets service")

