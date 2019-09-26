# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def email_task(config, tasks):
    tag = config.params.get("head_tag")

    for task in tasks:
        to_address = task["worker"].pop("to-address")
        content = task["worker"].pop("content")
        subject = task["worker"].pop("subject")
        link_text = task["worker"].pop("link-text")
        task["scopes"] = ["queue:route:notify.email.{}.on-completed".format(to_address)]
        task["routes"] = ["notify.email.{}.on-completed".format(to_address)]
        task["extra"] = {
            "notify": {
                "email": {
                    "content": content.format(tag=tag),
                    "subject": subject.format(tag=tag),
                    "link": {
                        "href": {"artifact-reference": "<sign/public/build/target.apk>"},
                        "text": link_text.format(tag=tag)
                    },
                }
            }
        }

        yield task
