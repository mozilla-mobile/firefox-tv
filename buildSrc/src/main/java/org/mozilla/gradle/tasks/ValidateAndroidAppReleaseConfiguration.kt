/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.mozilla.gradle.GitAggregates
import org.mozilla.gradle.ext.androidDSLOrThrow

/**
 * Validates that an Android app is correctly configured for release. When created, this
 * task will automatically create dependencies upon the release tasks.
 *
 * There are three states this task can run in:
 * - Run all checks
 * - Run checks appropriate for pull requests (add `-PisPullRequest`)
 * - Run no checks, e.g. when debugging locally (add `-PnoValidate`)
 *
 * For a list of current checks, see [validateAndroidAppRelease].
 */
open class ValidateAndroidAppReleaseConfiguration : DefaultTask() {
    init {
        group = "Verification"
        description = "Validates an Android app is correctly configured for release"

        this.onlyIf { !project.hasProperty("noValidate") }

        project.gradle.projectsEvaluated {
            getAssembleReleaseTasks().forEach {
                it.dependsOn(this@ValidateAndroidAppReleaseConfiguration)
            }

            // The assemble release tasks run late during execution so we make
            getCompileReleaseTasks().forEach {
                it.mustRunAfter(this@ValidateAndroidAppReleaseConfiguration)
            }
        }
    }

    private fun getAssembleReleaseTasks() = project.tasks.filter { task -> task.name.let {
        it.startsWith("assemble") && it.endsWith("Release")
    } }

    private fun getCompileReleaseTasks() = project.tasks.filter { task -> task.name.let {
        it.startsWith("compile") && it.endsWith("ReleaseSources")
    } }

    @TaskAction
    fun validateAndroidAppRelease() {
        // If you update which validations run, update the class kdoc too.
        validateNoUncommittedChanges()

        if (!project.hasProperty("isPullRequest")) {
            validateBuildVersionIsGitTagVersion() // There is no version git tag on a PR.
        }
    }

    private fun validateNoUncommittedChanges() {
        if (GitAggregates.hasUncommittedChanges()) {
            throw IllegalStateException("Unable to build release builds with uncommitted changes")
        }
    }

    // May not work correctly if there are multiple git tags.
    // In practice, this is uncommon for app releases.
    @Suppress("ThrowsCount") // Throwing to stop gradle builds is necessary.
    private fun validateBuildVersionIsGitTagVersion() {
        fun getGitTagVersionName(): String {
            // Expected: "v1.1"
            val gitTag = GitAggregates.getCheckedOutGitTag()
                    ?: throw IllegalStateException("Expected top-most commit to have a git tag")
            if (gitTag.getOrNull(0) != 'v') {
                throw IllegalStateException("Expected git tag to be a version, e.g. 'v1.1'")
            }
            return gitTag.drop(1) // remove "v"
        }

        val buildVersionName = project.androidDSLOrThrow().extension.defaultConfig.versionName
        val gitTagVersionName = getGitTagVersionName()
        if (buildVersionName != gitTagVersionName) {
            throw IllegalStateException("Expected build.gradle versionName, $buildVersionName, " +
                    "to match git tag version name, $gitTagVersionName")
        }
    }
}
