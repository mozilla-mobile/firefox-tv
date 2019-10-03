# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence
from taskgraph.util.schema import resolve_keyed_by

transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def email_task(config, tasks):
    tag = config.params.get("head_tag")
    release_type = config.params.get("release_type")

    for task in tasks:
        resolve_keyed_by(task, "worker.content", task["name"], **{
            "release-type": release_type or "production"
        })

        if release_type == 'lat':
            task["dependencies"].pop("push")

        to_address = task["worker"].pop("to-address")
        content = task["worker"].pop("content")
        subject = task["worker"].pop("subject")
        task["scopes"] = ["queue:route:notify.email.{}.on-completed".format(to_address)]
        task["routes"] = ["notify.email.{}.on-completed".format(to_address)]
        task["extra"] = {
            "notify": {
                "email": {
                    "content": {"artifact-reference": content.format(tag=tag)},
                    "subject": subject.format(tag=tag),
                }
            }
        }

        yield task
