package org.mozilla.focus.telemetry

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TelemetryIntegrationTest {

    private lateinit var telemetrySpy: Telemetry

    @Before
    fun setup() {
        val telemetry = TelemetryFactory.createTelemetry(RuntimeEnvironment.application)
        telemetrySpy = spy(telemetry)
        TelemetryHolder.set(telemetrySpy)
    }

    @Test
    fun `WHEN session events are called on TelemetryWrapper THEN associated Telemetry methods should be called`() {
        TelemetryIntegration.startSession(RuntimeEnvironment.application)
        TelemetryIntegration.stopSession(RuntimeEnvironment.application)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(1)).recordSessionEnd(any())
    }

    /**
     * See comment in [TelemetryIntegration.stopSession]
     */
    @Test
    fun `WHEN TelemetryWrapper is called out of order THEN we should not crash`() {
        TelemetryIntegration.stopSession(RuntimeEnvironment.application)
        TelemetryIntegration.startSession(RuntimeEnvironment.application)
    }
}
