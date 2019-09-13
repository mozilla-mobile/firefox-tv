# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import json
import os

import taskcluster

from tasks import schedule_task_graph, TaskBuilder


def pr(builder, branch):
    return (taskcluster.slugId(), builder.craft_pr_task(branch)),


def master(builder):
    return (taskcluster.slugId(), builder.craft_master_task()),


def release(builder, tag, is_staging):
    build_task_id = taskcluster.slugId()
    return (
        (build_task_id, builder.craft_release_build_task(tag)),
        (taskcluster.slugId(), builder.craft_sign_for_github_task(build_task_id, is_staging)),
        (taskcluster.slugId(), builder.craft_amazon_task(build_task_id, is_staging)),
    )


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest='command')

    pr_parser = subparsers.add_parser('pull-request')
    pr_parser.add_argument('author')
    pr_parser.add_argument('branch')

    master_parser = subparsers.add_parser('master')
    master_parser.add_argument('author')

    release_parser = subparsers.add_parser('release')
    release_parser.add_argument('tag')

    result = parser.parse_args()
    command = result.command
    task_group_id = os.environ['TASK_ID']
    repo_url = os.environ['MOBILE_HEAD_REPOSITORY']
    commit = os.environ['MOBILE_HEAD_REV']
    if command == 'pull-request':
        builder = TaskBuilder(result.author, repo_url, commit, task_group_id)
        ordered_tasks = pr(builder, result.branch)
    elif command == 'master':
        builder = TaskBuilder(result.author, repo_url, commit, task_group_id)
        ordered_tasks = master(builder)
    elif command == 'release':
        is_staging = repo_url != 'https://github.com/mozilla-mobile/firefox-tv'
        builder = TaskBuilder('firefox-tv@mozilla.com', repo_url, commit, task_group_id)
        ordered_tasks = release(builder, result.tag, is_staging)
    else:
        raise ValueError('A command ("pull-request", "master" or "release") must be provided to '
                         'the decision task')

    # This file is used by chainOfTrust to verify that the signing/push tasks were created by
    # this decision task
    full_task_graph = schedule_task_graph(ordered_tasks)
    with open('task-graph.json', 'w') as f:
        json.dump(full_task_graph, f)

    # These files are needed to keep chainOfTrust happy. For more details, see:
    # https://github.com/mozilla-releng/scriptworker/pull/209/files#r184180585
    for file_names in ('actions.json', 'parameters.yml'):
        with open(file_names, 'w') as f:
            json.dump({}, f)  # Yaml is a super-set of JSON.


if __name__ == '__main__':
    main()
