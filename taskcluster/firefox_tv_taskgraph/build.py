from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def build_task(config, tasks):
    for task in tasks:
        script = task["worker"]["script"].format(
            repo_url="TODO",
            tag="TODO",
        )
        trimmed_script = "\n".join([line.strip() for line in script.split("\n") if line.strip()])
        bash_command = [
            "/bin/bash",
            "--login",
            "-c",
            "cat <<'SCRIPT' > ../script.sh && bash -e ../script.sh\n"
            "export TERM=dumb\n{}\nSCRIPT".format(trimmed_script)
        ]

        task["worker"] = {
            "max-run-time": 3600,
            "docker-image": "mozillamobile/firefox-tv:2.3",
            "command": bash_command,
            "artifacts": task["worker"]["artifacts"],
        }

        yield task
