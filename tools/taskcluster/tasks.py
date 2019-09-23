# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import datetime
import json
import os

import taskcluster


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


def artifact(artifact_type, absolute_path):
    return {
        'type': artifact_type,
        'path': absolute_path,
        'expires': taskcluster.stringDate(taskcluster.fromNow('1 year'))
    }


class TaskBuilder:
    def __init__(self, owner, repo_url, commit, task_group_id):
        self.owner = owner
        self.repo_url = repo_url
        self.commit = commit
        self.task_group_id = task_group_id

    def craft_pr_task(self, branch):
        script = '''
        git fetch {} {}
        git config advice.detachedHead false
        git checkout {}
        yes | sdkmanager --licenses
        ./gradlew -PisPullRequest clean assembleSystem assembleAndroidTest lint checkstyle ktlint pmd detekt test
        ./gradlew -Pcoverage jacocoSystemDebugTestReport
        ./tools/taskcluster/upload-coverage-report.sh
        ./tools/taskcluster/download-firebase-sdk.sh
        ./tools/taskcluster/google-firebase-testlab-login.sh
        ./tools/taskcluster/execute-firebase-test.sh system/debug app-system-debug model=sailfish,version=25,orientation=landscape
        '''.format(self.repo_url, branch, self.commit)

        return self._craft_shell_task(
            "Firefox for Amazon's Fire TV - Build - Pull Request",
            script,
            ['secrets:get:project/mobile/firefox-tv/tokens'],
            {
                'public/reports': artifact('directory', '/opt/firefox-tv/app/build/reports')
            }
        )

    def craft_master_task(self):
        script = '''
        git fetch {}
        git config advice.detachedHead false
        git checkout {}
        yes | sdkmanager --licenses
        ./gradlew -PisPullRequest clean assembleSystem assembleAndroidTest lint checkstyle ktlint pmd detekt test
        python ./tools/taskcluster/get-bitbar-token.py
        python ./tools/taskcluster/execute-bitbar-test.py system/debug app-system-debug
        '''.format(self.repo_url, self.commit)

        return self._craft_shell_task(
            "Firefox for Amazon's Fire TV - Build - Master",
            script,
            ['secrets:get:project/mobile/firefox-tv/tokens'],
            {
                'public': artifact('directory', '/opt/firefox-tv/app/build/reports')
            }
        )

    def _craft_shell_task(self, name, script, scopes, artifacts, chain_of_trust=False):
        # The script value here is probably produced from a python heredoc string, which means
        # it has unnecessary whitespace in it. This will iterate over each line and remove the
        # redundant whitespace
        trimmed_script = '\n'.join([line.strip() for line in script.split('\n') if line.strip()])
        bash_command = [
            '/bin/bash',
            '--login',
            '-c',
            'cat <<"SCRIPT" > ../script.sh && bash -e ../script.sh\n'
            'export TERM=dumb\n{}\nSCRIPT'.format(trimmed_script),
        ]

        return self._craft_base_task(name, {
            'provisionerId': 'aws-provisioner-v1',
            'workerType': 'github-worker',
            'scopes': scopes,
            'payload': {
                'maxRunTime': 3600,
                'image': 'mozillamobile/firefox-tv:2.3',
                'command': bash_command,
                'artifacts': artifacts,
                'features': {
                    'taskclusterProxy': True,
                    'chainOfTrust': chain_of_trust,
                }
            }
        })

    def _craft_base_task(self, name, extend_task, dependencies=None):
        return {
            'label': name,
            'attributes': {},
            'dependencies': {} if dependencies is None else dependencies,
            'task': dict({
                'taskGroupId': self.task_group_id,
                'schedulerId': 'taskcluster-github',
                'created': taskcluster.stringDate(datetime.datetime.now()),
                'deadline': taskcluster.stringDate(taskcluster.fromNow('1 day')),
                'metadata': {
                    'name': name,
                    'description': '',
                    'owner': self.owner,
                    'source': '{}/raw/{}/.taskcluster.yml'.format(self.repo_url, self.commit),
                },
            }, **extend_task),
        }
