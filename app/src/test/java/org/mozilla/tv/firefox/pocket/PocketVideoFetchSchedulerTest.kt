/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.work.WorkManager
import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PocketVideoFetchSchedulerTest {

    private lateinit var scheduler: PocketVideoFetchScheduler
    @MockK private lateinit var workManager: WorkManager

    private var isPocketEnabledByLocale: Boolean = true

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        scheduler = PocketVideoFetchScheduler { isPocketEnabledByLocale }
        isPocketEnabledByLocale = true
    }

    @Test
    fun `GIVEN fetch interval constants THEN start hour is less than end hour`() {
        assertTrue(PocketVideoFetchScheduler.FETCH_START_HOUR < PocketVideoFetchScheduler.FETCH_END_HOUR)
    }

    @Test
    fun `GIVEN backoff interval constants THEN min is less than max`() {
        assertTrue(PocketVideoFetchScheduler.BACKOFF_DELAY_MIN_MILLIS < PocketVideoFetchScheduler.BACKOFF_DELAY_MAX_MILLIS)
    }

    @Test
    fun `GIVEN Pocket is disabled by locale THEN WorkManager is never interacted with so a job is never queued`() {
        isPocketEnabledByLocale = false
        scheduler.schedulePocketBackgroundFetch(workManager)
        verify { workManager wasNot called }
    }
}
