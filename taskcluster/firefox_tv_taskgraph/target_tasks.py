# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.target_tasks import _target_task, standard_filter


def tag_filter(target_release_type, task, params):
    release_type = task.attributes.get("release-type")
    return standard_filter(task, params) and (release_type is None or release_type == target_release_type)


@_target_task("default")
def target_tasks_default(full_task_graph, parameters, graph_config):
    filter = standard_filter
    return [l for l, task in full_task_graph.tasks.iteritems() if filter(task, parameters)]


@_target_task("production")
def target_tasks_production(full_task_graph, parameters, graph_config):
    return [l for l, task in full_task_graph.tasks.iteritems() if tag_filter("production", task, parameters)]


@_target_task("lat")
def target_tasks_production(full_task_graph, parameters, graph_config):
    return [l for l, task in full_task_graph.tasks.iteritems() if tag_filter("lat", task, parameters)]
