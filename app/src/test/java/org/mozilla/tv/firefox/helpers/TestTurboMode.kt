/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import org.mozilla.tv.firefox.utils.TurboMode

/**
 * An implementation of [TurboMode] that does not persist state between
 * instances.
 */
class TestTurboMode(private var isEnabled: Boolean) : TurboMode {
    override fun isEnabled(): Boolean = isEnabled

    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
}
