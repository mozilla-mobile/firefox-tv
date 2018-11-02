/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.telemetry

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.utils.anyNonNull
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryIntegrationTest {
    private lateinit var appContext: Application
    private lateinit var telemetryIntegration: TelemetryIntegration
    private lateinit var telemetrySpy: Telemetry
    private lateinit var sentrySpy: SentryIntegration

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext()
        val telemetry = TelemetryFactory.createTelemetry(appContext)
        telemetrySpy = spy(telemetry)
        TelemetryHolder.set(telemetrySpy)
        sentrySpy = spy(SentryIntegration)
        telemetryIntegration = TestTelemetryIntegration(sentrySpy)
    }

    @Test
    fun `WHEN startSession and stopSession are called on TelemetryWrapper THEN associated Telemetry methods should be called`() {
        telemetryIntegration.startSession(appContext)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(0)).recordSessionEnd(any())

        telemetryIntegration.stopSession(appContext)
        verify(telemetrySpy, times(1)).recordSessionStart()
        verify(telemetrySpy, times(1)).recordSessionEnd(any())
    }

    @Test
    fun `WHEN TelemetryWrapper is called out of order THEN sentry should capture callstack`() {
        telemetryIntegration.stopSession(appContext)
        telemetryIntegration.startSession(appContext)

        verify(sentrySpy, times(1)).capture(anyNonNull())
    }

    @Test
    fun `GIVEN session is running WHEN stopSession is called twice in a row THEN sentry should capture callstack`() {
        telemetryIntegration.startSession(appContext)
        telemetryIntegration.stopSession(appContext)
        telemetryIntegration.stopSession(appContext)

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
