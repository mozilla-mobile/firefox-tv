/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("UNUSED_PARAMETER") // We expect methods without implementations.

package org.mozilla.tv.firefox.helpers

/**
 * A collection of methods whose functionality changes based on engine in the build variant: each method
 * declared here should have a counterpart for each other engine variant (e.g. System).
 */
object EngineVariantFunctionality {

    /** Does nothing: see implementation in other variants. */
    fun preventCrashFromComponentsDefaultUserAgent(userAgentForTests: String) { }
}
