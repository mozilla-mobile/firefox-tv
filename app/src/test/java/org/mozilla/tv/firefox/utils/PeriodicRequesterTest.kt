package org.mozilla.tv.firefox.utils

import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class PeriodicRequesterTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var periodicRequester: PeriodicRequester<String>
    private lateinit var fakeEndpoint: FakeEndpoint

    private class FakeEndpoint : Endpoint<String> {
        var shouldSucceed = false
        var requestCount = 0
            private set

        override fun request(): Single<Response<String>> {
            requestCount++
            return when (shouldSucceed) {
                true -> Single.just(Response.Success(""))
                false -> Single.just(Response.Failure())
            }
        }
    }

    @Before
    fun setup() {
        testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxJavaPlugins.setIoSchedulerHandler { testScheduler }

        fakeEndpoint = FakeEndpoint()
        periodicRequester = PeriodicRequester(fakeEndpoint)
    }

    @Test
    fun `WHEN backoffTimes emits it SHOULD include all powers of 2 that add up to be smaller than 45 * 60`() {
        // 45 minutes * 60 seconds == 2700
        // 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 + 512 + 1024 == 2046
        val expected = listOf<Long>(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)
        val actual = periodicRequester.backoffTimes().toList().blockingGet().toList()

        assertEquals(expected, actual)
    }

    @Test
    fun `WHEN normal timer is started THEN it should immediately emit`() {
        val testNormal = periodicRequester.normalTimer().test()

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertEquals(1, testNormal.events.first().size)
    }

    @Test
    fun `GIVEN normal timer has been started WHEN 45 minutes pass THEN it should emit again`() {
        val testNormal = periodicRequester.normalTimer().test()

        testScheduler.advanceTimeTo(44, TimeUnit.MINUTES)
        assertEquals(1, testNormal.events.first().size)

        testScheduler.advanceTimeTo(45, TimeUnit.MINUTES)
        assertEquals(2, testNormal.events.first().size)

        testScheduler.advanceTimeTo(89, TimeUnit.MINUTES)
        assertEquals(2, testNormal.events.first().size)

        testScheduler.advanceTimeTo(90, TimeUnit.MINUTES)
        assertEquals(3, testNormal.events.first().size)
    }

    @Test
    fun `WHEN backoff timer has started THEN it should emit after pauses set by backoffTimes`() {
        val testBackoff = periodicRequester.backoffTimer().test()
        val backoffTimes = periodicRequester.backoffTimes().toList().blockingGet().toList()

        var expectedEmissions = 0
        backoffTimes.forEach { wait ->
            testScheduler.advanceTimeBy(wait, TimeUnit.SECONDS)
            assertEquals(++expectedEmissions, testBackoff.events.first().size)
        }
    }

    @Test
    fun `GIVEN requests fail WHEN merged has started THEN requests should be made on both normal and backoff timers`() {
        periodicRequester.start().subscribe()

        var expectedCalls = 1
        testScheduler.advanceTimeTo(1, TimeUnit.MILLISECONDS)
        assertEquals(expectedCalls, fakeEndpoint.requestCount)

        val expected = listOf<Long>(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)
        expected.forEach { wait ->
            testScheduler.advanceTimeBy(wait, TimeUnit.SECONDS)
            assertEquals(++expectedCalls, fakeEndpoint.requestCount)
        }

        testScheduler.advanceTimeTo(45, TimeUnit.MINUTES)
        assertEquals(++expectedCalls, fakeEndpoint.requestCount)

        expected.forEach { wait ->
            testScheduler.advanceTimeBy(wait, TimeUnit.SECONDS)
            assertEquals(++expectedCalls, fakeEndpoint.requestCount)
        }
    }

    @Test
    fun `GIVEN requests succeed WHEN merged has started THEN only one request should be made every 45 minutes`() {
        periodicRequester.start().subscribe()
        fakeEndpoint.shouldSucceed = true

        testScheduler.advanceTimeTo(1, TimeUnit.MILLISECONDS)
        assertEquals(1, fakeEndpoint.requestCount)

        testScheduler.advanceTimeTo(10, TimeUnit.MINUTES)
        assertEquals(1, fakeEndpoint.requestCount)

        testScheduler.advanceTimeTo(45, TimeUnit.MINUTES)
        assertEquals(2, fakeEndpoint.requestCount)

        testScheduler.advanceTimeTo(50, TimeUnit.MINUTES)
        assertEquals(2, fakeEndpoint.requestCount)

        testScheduler.advanceTimeTo(90, TimeUnit.MINUTES)
        assertEquals(3, fakeEndpoint.requestCount)
    }

    @Test
    fun `GIVEN requests have been failing AND merged has started WHEN requests succeed THEN backoff requests should stop`() {
        periodicRequester.start().subscribe()
        fakeEndpoint.shouldSucceed = false

        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS)
        assertEquals(3, fakeEndpoint.requestCount)

        fakeEndpoint.shouldSucceed = true

        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS)
        assertEquals(4, fakeEndpoint.requestCount)

        testScheduler.advanceTimeBy(16, TimeUnit.SECONDS)
        assertEquals(4, fakeEndpoint.requestCount)

        testScheduler.advanceTimeTo(44, TimeUnit.SECONDS)
        assertEquals(4, fakeEndpoint.requestCount)
    }
}
