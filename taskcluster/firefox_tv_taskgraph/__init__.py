# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from importlib import import_module
import os

from taskgraph.parameters import extend_parameters_schema
from voluptuous import Optional


def register(graph_config):
    """
    Import all modules that are siblings of this one, triggering decorators in
    the process.
    """
    _import_modules(["worker_types", "target_tasks"])
    extend_parameters_schema({
        Optional("head_tag"): basestring,
    })


def _import_modules(modules):
    for module in modules:
        import_module(".{}".format(module), package=__name__)


def get_decision_parametears(graph_config, parameters):
    parameters["head_tag"] = os.environ.get("MOBILE_HEAD_TAG")
