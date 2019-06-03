/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("UNUSED_PARAMETER") // We expect methods without implementations.

package org.mozilla.tv.firefox.helpers

import mozilla.components.browser.engine.system.SystemEngine
import org.mozilla.tv.firefox.TestFirefoxApplication

/**
 * A collection of methods whose functionality changes based on engine in the build variant: each method
 * declared here should have a counterpart for each other engine variant (e.g. Gecko).
 */
object EngineVariantFunctionality {

    /**
     * Overrides the a-c default user agent. Without doing this, the a-c implementation will call into `WebSettings`,
     * which throws an UnsupportedOperationException during Robolectric tests. This global variable is provided by a-c
     * to prevent this functionality. Note that we must also override similar behavior in our own code: see
     * [TestFirefoxApplication.getSystemUserAgent].
     */
    fun preventCrashFromComponentsDefaultUserAgent(userAgentForTests: String) {
        SystemEngine.defaultUserAgent = userAgentForTests
    }
}
