package org.mozilla.focus.telemetry

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.focus.utils.anyNonNull
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TelemetryIntegrationTest {

    private lateinit var telemetryIntegration: TelemetryIntegration
    private lateinit var telemetrySpy: Telemetry
    private lateinit var sentrySpy: SentryIntegration

    @Before
    fun setup() {
        val telemetry = TelemetryFactory.createTelemetry(RuntimeEnvironment.application)
        telemetrySpy = spy(telemetry)
        TelemetryHolder.set(telemetrySpy)
        sentrySpy = spy(SentryIntegration)
        telemetryIntegration = TestTelemetryIntegration(sentrySpy)
    }

    @Test
    fun `WHEN session events are called on TelemetryWrapper THEN associated Telemetry methods should be called`() {
        telemetryIntegration.startSession(RuntimeEnvironment.application)
        telemetryIntegration.stopSession(RuntimeEnvironment.application)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(1)).recordSessionEnd(any())
    }

    @Test
    fun `WHEN TelemetryWrapper is called out of order THEN sentry should capture callstack`() {
        telemetryIntegration.stopSession(RuntimeEnvironment.application)
        telemetryIntegration.startSession(RuntimeEnvironment.application)

        verify(sentrySpy, times(1)).capture(anyNonNull())
    }
}

/**
 * Allows us to pass a non-default value for [SentryIntegration] for testing
 * purposes
 */
private class TestTelemetryIntegration(
        sentryIntegration: SentryIntegration
) : TelemetryIntegration(sentryIntegration)
