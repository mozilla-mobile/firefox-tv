/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle.ext

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

fun TaskContainer.getAssembleReleaseTasks(): List<Task> = filter { task -> task.name.let {
    it.startsWith("assemble") && it.endsWith("Release")
} }

fun TaskContainer.getCompileReleaseTasks(): List<Task> = filter { task -> task.name.let {
    it.startsWith("compile") && it.endsWith("ReleaseSources")
} }
