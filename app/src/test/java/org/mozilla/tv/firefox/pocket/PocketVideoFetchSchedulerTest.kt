/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.junit.Assert.assertTrue
import org.junit.Test

class PocketVideoFetchSchedulerTest {

    @Test
    fun `GIVEN fetch interval constants THEN start hour is less than end hour`() {
        assertTrue(PocketVideoFetchScheduler.FETCH_START_HOUR < PocketVideoFetchScheduler.FETCH_END_HOUR)
    }

}
