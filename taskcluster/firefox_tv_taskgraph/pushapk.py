from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


@transforms.add
def pushapk_task(config, tasks):
    for task in tasks:
        task["worker"]["dep"] = config.params["level"] != 3
        yield task
