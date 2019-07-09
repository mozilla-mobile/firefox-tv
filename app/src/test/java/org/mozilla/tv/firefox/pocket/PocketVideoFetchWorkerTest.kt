/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.utils.ServiceLocator

class PocketVideoFetchWorkerTest {

    @Rule
    @JvmField
    var timeout = Timeout.seconds(20)

    private lateinit var worker: PocketVideoFetchWorker

    @MockK private lateinit var context: FirefoxApplication
    @Suppress("DEPRECATION") // We need endpoint raw until we move to the a-c impl.
    @MockK private lateinit var endpointRaw: PocketEndpointRaw
    @MockK private lateinit var store: PocketVideoStore

    @MockK private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { context.applicationContext } returns context
        every { context.serviceLocator } returns mockk<ServiceLocator>().also {
            every { it.pocketEndpointRaw } returns endpointRaw
            every { it.pocketVideoStore } returns store
        }

        worker = PocketVideoFetchWorker(context, workerParams)
    }

    @Test
    fun `WHEN the endpoint fails to return a result THEN the job will retry`() {
        coEvery { endpointRaw.getGlobalVideoRecommendations() } returns null
        assertEquals(Result.retry(), worker.doWork())
    }

    @Test
    fun `WHEN the endpoint returns a non-null result THEN the store decides what happens to the exact json string`() {
        arrayOf(
            "",
            "{",
            "{}",
            "{\"json\": \"yeah!\"}"
        ).forEachIndexed { i, json ->
            coEvery { endpointRaw.getGlobalVideoRecommendations() } returns json
            every { store.save(any()) } returns true

            worker.doWork()

            println("For index $i: $json")
            verify { store.save(json) }
        }
    }

    @Test
    fun `WHEN the store says the endpoint returned invalid json THEN the job will fail`() {
        // The store returns false if the JSON is invalid and refuses to be saved:
        // unfortunately, this naming is not very intuitive.
        everyEndpointRawGetVideoRecsReturnsANonNullValue()
        every { store.save(any()) } returns false
        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun `WHEN the endpoint returns valid json THEN the job will succeed`() {
        // The store returns true if the JSON is valid and can be saved:
        // unfortunately, this naming is not very intuitive.
        everyEndpointRawGetVideoRecsReturnsANonNullValue()
        every { store.save(any()) } returns true
        assertEquals(Result.success(), worker.doWork())
    }

    private fun everyEndpointRawGetVideoRecsReturnsANonNullValue() {
        // For many tests, we only need a non-null return value to advance to the next portion of the function,
        // which is the part we want to test.
        coEvery { endpointRaw.getGlobalVideoRecommendations() } returns "{}"
    }
}
