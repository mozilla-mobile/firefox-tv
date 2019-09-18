# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import taskcluster


def pr(builder, branch):
    return builder.craft_pr_task(branch),


def master(builder):
    return builder.craft_master_task(),


def release(builder, tag, is_staging):
    build_task = builder.craft_release_build_task(tag)
    sign_task = builder.craft_sign_for_github_task(build_task['label'], is_staging)
    push_task = builder.craft_amazon_task(build_task['label'], is_staging)
    return (
        build_task,
        sign_task,
        push_task,
        builder.craft_email_task(sign_task['label'], push_task['label'], tag),
    )
