import datetime
import json
import os

import taskcluster


def artifact(artifact_type, absolute_path):
    return {
        'type': artifact_type,
        'path': absolute_path,
        'expires': taskcluster.stringDate(taskcluster.fromNow('1 week'))
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
                'public/reports': artifact('directory', '/opt/firefox-tv/app/builds/reports')
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
                'public': artifact('directory', '/opt/firefox-tv/app/builds/reports')
            }
        )

    def craft_release_task(self, tag):
        script = f'''
        git fetch origin --tags
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
            }
        )

    def _craft_shell_task(self, name, script, scopes, artifacts):
        single_command = ' && '.join([line.strip() for line in script.split('\n') if line.strip()])
        bash_command = [
            '/bin/bash',
            '--login',
            '-cx',
            f'export TERM=dumb && {single_command}'
        ]

        return {
            'taskGroupId': self.task_group_id,
            'provisionerId': 'aws-provisioner-v1',
            'schedulerId': 'taskcluster-github',
            'workerType': 'github-worker',
            'created': taskcluster.stringDate(datetime.datetime.now()),
            'deadline': taskcluster.stringDate(taskcluster.fromNow('1 day')),
            'scopes': scopes,
            'payload': {
                'maxRunTime': 3600,
                'image': 'mozillamobile/firefox-tv:2.3',
                'command': bash_command,
                'artifacts': artifacts,
                'features': {
                    'taskclusterProxy': True,
                }
            },
            'metadata': {
                'name': name,
                'description': '',
                'owner': self.owner,
                'source': f'{self.repo_url}/raw/{self.commit}/.taskcluster.yml',
            }
        }


def schedule_task_graph(ordered_tasks):
    queue = taskcluster.Queue({'rootUrl': os.environ['TASKCLUSTER_PROXY_URL']})
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
