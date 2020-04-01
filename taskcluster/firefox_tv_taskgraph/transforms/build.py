# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


@transforms.add
def expose_artifacts_in_attributes(config, tasks):
    for task in tasks:
        task.setdefault("attributes", {})
        task["attributes"]["apks"] = [artifact["name"] for artifact in task["worker"].get("artifacts", [])]
        yield task
