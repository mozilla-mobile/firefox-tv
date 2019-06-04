/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gradle

/**
 * A collection of functionality related to our application and buildscript dependencies.
 */
object Dependencies {

    /**
     * Functionality to limit specific dependencies to specific repositories. These are typically expected to be used by
     * dependency group name (i.e. with `include/excludeGroup`). For additional info, see:
     * https://docs.gradle.org/current/userguide/declaring_repositories.html#sec::matching_repositories_to_dependencies
     */
    object RepoMatching {
        const val mozilla = "org\\.mozilla\\..*"
        const val androidx = "androidx\\..*"
        const val comAndroid = "com\\.android\\..*"

        /**
         * A matcher for com.google.android.* with one exception: the espresso-contrib dependency includes the
         * accessibility-test-framework dependency, which is not available in the google repo. As such, we must
         * explicitly exclude it from this regex so it can be found on jcenter. Note that the transitive dependency
         * com.google.guava is also not available on google's repo.
         */
        const val comGoogleAndroid = "com\\.google\\.android\\.(?!apps.common.testing.accessibility.frame).*"
    }
}
