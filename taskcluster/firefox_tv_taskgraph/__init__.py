# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from importlib import import_module
import os

from six import text_type
from taskgraph.parameters import extend_parameters_schema
from voluptuous import Required, Any


def register(graph_config):
    """
    Import all modules that are siblings of this one, triggering decorators in
    the process.
    """
    _import_modules(["job", "target_tasks", "worker_types"])
    extend_parameters_schema({
        Required("release_type", default=None): Any(text_type, None),
    })


def _import_modules(modules):
    for module in modules:
        import_module(".{}".format(module), package=__name__)


def get_decision_parameters(graph_config, parameters):
    if parameters["tasks_for"] == "github-release":
        release_type = "lat" if "LAT" in parameters["head_tag"] else "production"
        parameters["release_type"] = release_type
        parameters["target_tasks_method"] = "lat" if release_type == "lat" else "production"
