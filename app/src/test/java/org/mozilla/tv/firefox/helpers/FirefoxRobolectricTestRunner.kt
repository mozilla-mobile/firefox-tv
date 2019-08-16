/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import org.mozilla.tv.firefox.helpers.shadows.ShadowADM
import org.mozilla.tv.firefox.helpers.shadows.ShadowADMMessageHandlerBase
import org.mozilla.tv.firefox.helpers.shadows.ShadowADMMessageReceiver
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test runner that initializes the test environment for our unit tests.
 */

class FirefoxRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {

    override fun buildGlobalConfig(): Config {
        val defaultConfig = super.buildGlobalConfig()

        // See [ShadowADMMessageReceiver] and [ShadowADMMessageHandlerBase] for reason why these shadows are needed
        val shadows = defaultConfig.shadows +
            arrayOf(ShadowADMMessageHandlerBase::class, ShadowADMMessageReceiver::class, ShadowADM::class)

        return Config.Builder(defaultConfig)
            .setShadows(shadows.map { it.java }.toTypedArray())
            .build()
    }
}
