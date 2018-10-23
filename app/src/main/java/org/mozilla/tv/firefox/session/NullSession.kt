/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.session

import mozilla.components.browser.session.Session

/**
 * Helper class for migrating to browser-session component. Eventually this class should get
 * refactored away. It only exists to make code depending on a "NullSession" easier to migrate.
 * At the latest when introducing the browser-engine component this should not be needed anymore.
 */
class NullSession private constructor() {
    companion object {
        @JvmStatic
        fun create(): Session = Session("about:blank")
    }
}
