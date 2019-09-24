from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


NOTIFY_EMAIL_ADDRESS = 'firefox-tv@mozilla.com'


@transforms.add
def build_task(config, tasks):
    for task in tasks:
        script = task["worker"]["script"].format(
            repo_url=config.params["head_repository"],
            commit=config.params["head_rev"],  # TODO use params?
            tag=os.environ.get("GIT_TAG"),  # TODO use params?
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

        yield task
