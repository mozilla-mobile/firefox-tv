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
        script = f'''
        git fetch {self.repo_url} {branch}
        git config advice.detachedHead false
        git checkout {self.commit}
        yes | sdkmanager --licenses
        ./gradlew -PisPullRequest clean assembleSystem assembleAndroidTest lint checkstyle ktlint pmd detekt test
        ./gradlew -Pcoverage jacocoSystemDebugTestReport
        ./tools/taskcluster/upload-coverage-report.sh
        ./tools/taskcluster/download-firebase-sdk.sh
        ./tools/taskcluster/google-firebase-testlab-login.sh
        ./tools/taskcluster/execute-firebase-test.sh system/debug app-system-debug model=sailfish,version=25,orientation=landscape
        '''

        return self._craft_shell_task(
            "Firefox for Amazon's Fire TV - Build - Pull Request",
            script,
            ['secrets:get:project/mobile/firefox-tv/tokens'],
            {
                'public/reports': artifact('directory', '/opt/firefox-tv/app/build/reports')
            }
        )

    def craft_master_task(self):
        script = f'''
        git fetch {self.repo_url}
        git config advice.detachedHead false
        git checkout {self.commit}
        yes | sdkmanager --licenses
        ./gradlew -PisPullRequest clean assembleSystem assembleAndroidTest lint checkstyle ktlint pmd detekt test
        python ./tools/taskcluster/get-bitbar-token.py
        python ./tools/taskcluster/execute-bitbar-test.py system/debug app-system-debug
        '''

        return self._craft_shell_task(
            "Firefox for Amazon's Fire TV - Build - Master",
            script,
            ['secrets:get:project/mobile/firefox-tv/tokens'],
            {
                'public': artifact('directory', '/opt/firefox-tv/app/build/reports')
            }
        )

    def craft_release_build_task(self, tag):
        script = f'''
        git fetch {self.repo_url} --tags
        git config advice.detachedHead false
        git checkout {tag}
        yes | sdkmanager --licenses
        python tools/taskcluster/get-sentry-token.py
        python tools/taskcluster/get-pocket-token.py
        ./gradlew --no-daemon clean test assembleSystemRelease
        '''

        return self._craft_shell_task(
            f'Firefox for Fire TV - Release build {tag}',
            script,
            ['secrets:get:project/mobile/firefox-tv/tokens'],
            {
                'public': artifact('directory', '/opt/firefox-tv/app/build/outputs/apk')
            },
            chain_of_trust=True,  # Needed for sign and push task verification
        )

    def craft_email_task(self, sign_task_id, push_task_id, tag):
        # The "\\n" are hard-coded on purpose, since we don't want a newline in the string, but
        # we do want the JSON to have the escaped newline.
        # This email content is formatted with markdown by Taskcluster
        content = 'Automation for this release is ready. Please: \\n' \
                  f'* Download the APK and attach it to the [Github release](https://github.com/mozillamobile/firefox-tv/releases/tag/{tag})\\n' \
                  '* [Deploy the new release on Amazon](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.7f334089688646ef8953d041021029c9/release/amzn1.devportal.apprelease.4ca3990c43f34101bf5729543343747a/general/detail)'
        script = f'''
        curl -X POST "$TASKCLUSTER_PROXY_URL/notify/v1/email" -H "Content-Type: application/json" -d "$(cat <<EOM
        {{
            "address": "{NOTIFY_EMAIL_ADDRESS}",
            "content": "{content}",
            "link": {{
                "href": "https://queue.taskcluster.net/v1/task/{sign_task_id}/artifacts/public/build/target.apk",
                "text": "{tag} APK"
            }},
            "subject": "Release {tag} is ready for deployment"
        }}
        EOM
        )"
        '''

        return self._craft_shell_task(
            'Email that automation is complete',
            script,
            [f'notify:email:{NOTIFY_EMAIL_ADDRESS}'],
            {},
            dependencies=[sign_task_id, push_task_id],
        )

    def _craft_shell_task(self, name, script, scopes, artifacts, *, dependencies=(), chain_of_trust=False):
        trimmed_script = '\n'.join([line.strip() for line in script.split('\n') if line.strip()])
        bash_command = [
            '/bin/bash',
            '--login',
            '-c',
            'cat <<"SCRIPT" script.sh && bash -e script.sh\n'
            'export TERM=dumb\n'
            f'{trimmed_script}\n'
            'SCRIPT'
        ]

        return self._craft_base_task(name, {
            'provisionerId': 'aws-provisioner-v1',
            'workerType': 'github-worker',
            'scopes': scopes,
            'dependencies': dependencies,
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

    def craft_sign_for_github_task(self, build_task_id, is_staging):
        return self._craft_base_task('Sign for Github', {
            'provisionerId': 'scriptworker-prov-v1',
            'workerType': 'mobile-signing-dep-v1' if is_staging else 'mobile-signing-v1',
            'scopes': [
                'project:mobile:firefox-tv:releng:signing:format:autograph_apk',
                'project:mobile:firefox-tv:releng:signing:cert:{}-signing'.format(
                    'dep' if is_staging else 'production')
            ],
            'dependencies': [build_task_id],
            'payload': {
                'upstreamArtifacts': [{
                    'paths': ['public/build/target.apk'],
                    'formats': ['autograph_apk'],
                    'taskId': build_task_id,
                    'taskType': 'build',
                }]
            },
        })

    def craft_amazon_task(self, build_task_id, is_staging):
        return self._craft_base_task('Push to Amazon', {
            'provisionerId': 'scriptworker-prov-v1',
            'workerType': 'mobile-pushapk-dep-v1' if is_staging else 'mobile-pushapk-v1',
            'scopes': [
                'project:mobile:firefox-tv:releng:googleplay:product:firefox-tv{}'.format(
                    ':dep' if is_staging else ''
                )
            ],
            'dependencies': [build_task_id],
            'payload': {
                'target_store': 'amazon',
                'channel': 'production',
                'upstreamArtifacts': [{
                    'paths': ['public/build/target.apk'],
                    'taskId': build_task_id,
                    'taskType': 'build',
                }]
            }
        })

    def _craft_base_task(self, name, extend_task):
        return {
            'taskGroupId': self.task_group_id,
            'schedulerId': 'taskcluster-github',
            'created': taskcluster.stringDate(datetime.datetime.now()),
            'deadline': taskcluster.stringDate(taskcluster.fromNow('1 day')),
            'metadata': {
                'name': name,
                'description': '',
                'owner': self.owner,
                'source': f'{self.repo_url}/raw/{self.commit}/.taskcluster.yml',
            },
            **extend_task
        }


def schedule_task_graph(ordered_tasks):
    queue = taskcluster.Queue({'rootUrl': os.environ.get('TASKCLUSTER_PROXY_URL', 'https://taskcluster.net')})
    full_task_graph = {}

    for task_id, task in ordered_tasks:
        print("TASK", task_id)
        print(json.dumps(task, indent=4, separators=(',', ': ')))

        result = queue.createTask(task_id, task)
        print("RESULT", task_id)
        print(json.dumps(result))

        full_task_graph[task_id] = {
            # Some values of the task definition are automatically filled. Querying the task
            # allows to have the full definition. This is needed to make Chain of Trust happy
            'task': queue.task(task_id),
        }

    return full_task_graph
