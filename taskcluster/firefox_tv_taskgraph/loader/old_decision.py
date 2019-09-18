from __future__ import print_function, unicode_literals

import os
import sys

current_dir = os.path.dirname(os.path.realpath(__file__))
project_dir = os.path.realpath(os.path.join(current_dir, '..', '..', '..'))
sys.path.append(project_dir)


from tools.taskcluster.decision_task import (
    pr,
    master,
    release,
)
from tools.taskcluster.tasks import TaskBuilder


def loader(kind, path, config, params, loaded_tasks):
    repo_url = params['head_repository']
    commit = params['head_rev']
    trust_level = int(params['level'])

    builder = TaskBuilder(params['owner'], repo_url, commit, os.environ['TASK_ID'])
    is_staging = trust_level != 3

    tasks_for = params['tasks_for']
    if tasks_for == 'github-pull-request':
        ordered_groups_of_tasks = pr(builder, params['head_ref'])
    elif tasks_for == 'github-push' and params['head_ref'] == 'master':
        ordered_groups_of_tasks = master(builder)
    elif tasks_for == 'github-release':
        ordered_groups_of_tasks = release(builder, os.environ['GIT_TAG'], is_staging)
    else:
        print("This decision task isn't for a PR, push to master, or release, so no follow-up "
              "tasks will be scheduled")
        return

    for task in ordered_groups_of_tasks:
        yield task
