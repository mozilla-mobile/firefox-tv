/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle

import org.mozilla.gradle.ext.execWaitForStdOut

/** Git state retrieved from raw Git commands. */
object GitAggregates {

    fun hasUncommittedChanges(): Boolean = !Git.status().isBlank()

    /** @return the checked out git tag, or null if there is none */
    fun getCheckedOutGitTag(): String? {
        val commandOutput = Git.nameRev("--tags", "HEAD").trim() // ends in whitespace.

        // "HEAD undefined" if there is no git tag on HEAD.
        if (commandOutput.endsWith("undefined")) {
            return null
        }

        // "HEAD tags/v1.1" if there is a git tag on HEAD.
        return commandOutput.split("/")[1]
    }
}

/** Raw Git commands. */
object Git {
    private val runtime: Runtime
        get() = Runtime.getRuntime()

    fun status(): String = runtime.execWaitForStdOut("git status --porcelain")

    fun nameRev(vararg args: String): String =
            Runtime.getRuntime().execWaitForStdOut("git name-rev ${args.joinToString(" ")}")
}
