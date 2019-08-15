/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class MainActivityTest {

    // sanity check:
    // 1) we need at least one start up test to ensure we don't regress Robolectric.buildActivity working correctly.
    // 2) devs run unit tests before pushing but generally don't run UI tests so we can tighten their feedback loop
    // here, in unit tests, by alerting them if MainActivity crashes on regular startup.
    @Test
    fun `WHEN MainActivity starts up regularly THEN it does not crash`() {
        Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
    }
}
