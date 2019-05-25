/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class PocketVideoFetchSchedulerTest {

    private lateinit var scheduler: PocketVideoFetchScheduler
    @MockK(relaxed = true) private lateinit var workManager: WorkManager

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

    @Ignore // TODO: the WorkManager dependency breaks calling onStart; after we commit the other tests, fix this test.
    @Test
    fun `WHEN onStart is called THEN schedulePocketBackgroundFetch is called`() {
        // This test is tightly coupled to the implementation but, given this daily background job will rarely be
        // tested, we want to verify the implementation hasn't accidentally changed in a way that broke the functionality.
        val schedulerMock = mockk<PocketVideoFetchScheduler>(relaxUnitFun = true)
        schedulerMock.onStart()
        verify(exactly = 1) { schedulerMock.schedulePocketBackgroundFetch(any()) }
    }

    @Test
    fun `GIVEN Pocket is disabled by locale THEN WorkManager is never interacted with so a job is never queued`() {
        isPocketEnabledByLocale = false
        scheduler.schedulePocketBackgroundFetch(workManager)
        verify { workManager wasNot called }
    }

    @Test
    fun `GIVEN Pocket is enabled by locale THEN WorkManager will enqueue a request with a connected NetworkType`() {
        scheduler.schedulePocketBackgroundFetch(workManager)
        val request = verifyWorkManagerEnqueueAndCaptureRequest()
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `GIVEN Pocket is enabled by locale THEN WorkManager will enqueue a request using the keep existing work policy`() {
        scheduler.schedulePocketBackgroundFetch(workManager)
        val policy = slot<ExistingWorkPolicy>()
        verify { workManager.enqueueUniqueWork(any(), capture(policy), any() as OneTimeWorkRequest) }
        assertEquals(ExistingWorkPolicy.KEEP, policy.captured)
    }

    @Test
    fun `GIVEN Pocket is enabled by locale THEN WorkManager will enqueue a request with an exponential backoff criteria`() {
        scheduler.schedulePocketBackgroundFetch(workManager)
        val request = verifyWorkManagerEnqueueAndCaptureRequest()
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
    }

    @Test
    fun `GIVEN Pocket is enabled by locale THEN WorkManager will enqueue a request with a backoff delay based on randomness`() {
        // For these tests, we assume the returned value from the random number generator is used directly.
        var fromArg = 0L
        scheduler.schedulePocketBackgroundFetch(workManager, randLong = { from, _ -> from.also { fromArg = from } })
        val request = verifyWorkManagerEnqueueAndCaptureRequest()
        assertEquals(fromArg, request.workSpec.backoffDelayDuration)

        var untilArg = 0L
        scheduler.schedulePocketBackgroundFetch(workManager, randLong = { _, until -> until.also { untilArg = until } })
        val request2 = verifyWorkManagerEnqueueAndCaptureRequest()
        assertEquals(untilArg, request2.workSpec.backoffDelayDuration)

        assertNotEquals(fromArg, untilArg) // Sanity check to verify test correctness on the tricky capturing above.
    }

    // TODO: test initial delay in work request
    // TODO: test getDelayUntilUpcomingNightFetchMillis logic (should we test it directly or as part of the request?)

    private fun verifyWorkManagerEnqueueAndCaptureRequest(): OneTimeWorkRequest = slot<OneTimeWorkRequest>().also {
        verify { workManager.enqueueUniqueWork(any(), any(), capture(it)) }
        assertTrue(it.isCaptured)
    }.captured
}
