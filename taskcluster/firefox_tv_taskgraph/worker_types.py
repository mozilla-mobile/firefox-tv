from __future__ import absolute_import, print_function, unicode_literals

from six import text_type

from voluptuous import Required

from taskgraph.util.schema import taskref_or_string
from taskgraph.transforms.task import payload_builder


@payload_builder(
    "scriptworker-signing",
    schema={
        Required("max-run-time"): int,
        Required("signing-type"): text_type,
        Required("upstream-artifacts"): [{
            Required("taskId"): taskref_or_string,
            Required("taskType"): text_type,
            Required("paths"): [text_type],
            Required("formats"): [text_type],
        }]
    }
)
def build_scriptworker_signing_payload(config, task, task_def):
    worker = task["worker"]
    task_def["tags"]["worker-implementation"] = "scriptworker"
    task_def["payload"] = {
        "maxRunTime": worker["max-run-time"],
        "upstreamArtifacts": worker["upstream-artifacts"],
    }

    formats = set()
    for artifacts in worker["upstream-artifacts"]:
        formats.update(artifacts["formats"])

    scope_prefix = config.graph_config["scriptworker"]["scope-prefix"]
    task_def["scopes"].append(
        "{}:signing:cert:{}".format(scope_prefix, worker["signing-type"])
    )
    task_def["scopes"].extend([
        "{}:signing:format:{}".format(scope_prefix, signing_format) for signing_format in sorted(formats)
    ])


@payload_builder(
    "scriptworker-pushapk",
    schema={
        Required("upstream-artifacts"): [{
            Required("taskId"): taskref_or_string,
            Required("taskType"): text_type,
            Required("paths"): [text_type],
        }],
        Required("channel"): text_type,
    }
)
def build_push_apk_payload(config, task, task_def):
    worker = task["worker"]
    task_def["tags"]["worker-implementation"] = "scriptworker"

    task_def["payload"] = {
        "target_store": worker["target-store"],
        "upstreamArtifacts": worker["upstream-artifacts"],
        "channel": worker["channel"],
    }

    scope_prefix = config.graph_config["scriptworker"]["scope-prefix"]
    task_def["scopes"].append(
        "{}:googleplay:product:{}{}".format(
            scope_prefix, worker["product"], ":dep" if worker["dep"] else ""
        )
    )
