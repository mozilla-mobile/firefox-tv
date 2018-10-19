/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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
    fun `WHEN startSession and stopSession are called on TelemetryWrapper THEN associated Telemetry methods should be called`() {
        telemetryIntegration.startSession(RuntimeEnvironment.application)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(0)).recordSessionEnd(any())

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

    @Test
    fun `GIVEN session is running WHEN stopSession is called twice in a row THEN sentry should capture callstack`() {
        telemetryIntegration.startSession(RuntimeEnvironment.application)
        telemetryIntegration.stopSession(RuntimeEnvironment.application)
        telemetryIntegration.stopSession(RuntimeEnvironment.application)

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
