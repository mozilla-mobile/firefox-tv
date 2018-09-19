package org.mozilla.focus.telemetry

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TelemetryWrapperTest {

    private lateinit var telemetrySpy: Telemetry

    @Before
    fun setup() {
        val telemetry = TelemetryFactory.createTelemetry(RuntimeEnvironment.application)
        telemetrySpy = spy(telemetry)
        TelemetryHolder.set(telemetrySpy)
    }

    @Test
    fun `WHEN session events are called on TelemetryWrapper THEN associated Telemetry methods should be called`() {
        TelemetryWrapper.startSession(RuntimeEnvironment.application)
        TelemetryWrapper.stopSession(RuntimeEnvironment.application)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(1)).recordSessionEnd()
    }

    /**
     * See comment in [TelemetryWrapper.stopSession]
     */
    @Test
    fun `WHEN TelemetryWrapper is called out of order THEN we should not crash`() {
        TelemetryWrapper.stopSession(RuntimeEnvironment.application)
        TelemetryWrapper.startSession(RuntimeEnvironment.application)
    }
}
