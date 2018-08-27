/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.session.SessionUseCases

/**
 * Helper class for lazily instantiating and keeping references to components needed by the
 * application.
 */
class Components {
    val engine: Engine by lazy { SystemEngine() }

    val sessionManager by lazy { SessionManager(engine) }

    val sessionUseCases by lazy { SessionUseCases(sessionManager) }
}
