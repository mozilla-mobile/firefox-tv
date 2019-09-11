/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.manual.upgrade

/**
 * TODO
 */
interface ManualUpgradeStarter {
    fun maybeShow()
}

class DoNotShowUpgradeStarter() : ManualUpgradeStarter {
    override fun maybeShow() { /* Noop */ }
}

class RequestUpgradeStarter() : ManualUpgradeStarter {
    override fun maybeShow() {
        // Check last time shown
        // If time > X, start request upgrade activity
        TODO()
    }
}

class ForceUpgradeStarter() : ManualUpgradeStarter {
    override fun maybeShow() {
        // start force upgrade activity
        TODO()
    }
}
