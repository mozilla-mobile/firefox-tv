/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SentryIntegrationTest {

    @Test
    fun `GIVEN the application is initialized THEN Sentry is init`() {
        // application.onCreate(): RobolectricTestRunner automatically calls this before every test.
        assertTrue(SentryIntegration.isInit)
    }
}
