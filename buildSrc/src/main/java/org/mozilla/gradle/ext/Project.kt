/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle.ext

import com.android.build.gradle.AppPlugin
import org.gradle.api.Project

fun Project.androidDSLOrThrow(): AppPlugin {
    return plugins.findPlugin("android") as? AppPlugin
            ?: throw IllegalStateException("Android plugin must be applied")
}
