from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def build_email_task(config, tasks):
    tag = os.environ['GIT_TAG']
    content = 'Automation for this release is ready. Please: \n' \
              '* Download the APK and attach it to the [Github release](https://github.com/mozilla-mobile/firefox-tv/releases/tag/{})\n'.format(tag) + \
              '* [Deploy the new release on Amazon](https://developer.amazon.com/apps-and-games/console/app/amzn1.devportal.mobileapp.7f334089688646ef8953d041021029c9/release/amzn1.devportal.apprelease.4ca3990c43f34101bf5729543343747a/general/detail)'

    for task in tasks:
        to_address = task["worker"]["to-address"]
        del task["worker"]["to-address"]
        task["scopes"] = ["queue:route:notify.email.{}.on-completed".format(to_address)]
        task["routes"] = ["notify.email.{}.on-completed".format(to_address)]
        task["extra"] = {
            "notify": {
                "email": {
                    "content": content,
                    "subject": "Release {} is ready for deployment".format(tag),
                    "link": {
                        "href": {"artifact-reference": "<sign/public/build/target.apk>"},
                        "text": "{} APK".format(tag)
                    },
                }
            }
        }

        yield task
