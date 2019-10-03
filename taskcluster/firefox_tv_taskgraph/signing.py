# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


@transforms.add
def signing_task(config, tasks):
    for task in tasks:
        task["worker"]["signing-type"] = 'dep-signing' if config.params["level"] != u'3' else 'production-signing'
        yield task


@transforms.add
def set_signing_format(config, tasks):
    for task in tasks:
        for upstream_artifact in task["worker"]["upstream-artifacts"]:
            upstream_artifact["formats"] = ["autograph_apk"]
        yield task
