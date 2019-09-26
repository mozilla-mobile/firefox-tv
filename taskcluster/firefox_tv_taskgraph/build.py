# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def build_task(config, tasks):
    for task in tasks:
        script = task["worker"]["script"].format(
            repo_url=config.params["head_repository"],
            commit=config.params["head_rev"],
            tag=config.params["head_tag"],
            branch=config.params["head_ref"],
        )
        bash_command = [
            "/bin/bash",
            "--login",
            "-c",
            "cat <<'SCRIPT' > ../script.sh && bash -e ../script.sh\n"
            "export TERM=dumb\n{}\nSCRIPT".format(script)
        ]

        del task["worker"]["script"]
        task["worker"]["command"] = bash_command
        task["worker"].setdefault("artifacts", [])
        task["worker"]["taskcluster-proxy"] = True
        task["worker"]["chain-of-trust"] = True

        yield task
