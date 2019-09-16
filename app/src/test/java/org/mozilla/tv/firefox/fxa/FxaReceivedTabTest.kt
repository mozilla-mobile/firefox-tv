/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.TabData
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.framework.UnresolvedString
import org.mozilla.tv.firefox.telemetry.SentryIntegration

class FxaReceivedTabTest {

    @MockK(relaxed = true) private lateinit var sentryIntegration: SentryIntegration

    @Before
    fun before() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN one receive tab event occurs with two URLs and a non-null device THEN the corresponding event is emitted`() {
        val expectedDeviceName = "Expected device name"
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast, listOf(expectedDeviceName)),
            FxaReceivedTab.Metadata(DeviceType.DESKTOP, 2)
        )

        val inputTabData = expectedTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEvent(DeviceType.DESKTOP, expectedDeviceName, inputTabData)

        Observable.just(tabReceivedEvent)
            .filterMapToDomainObject()
            .test()
            .assertValues(expected)
    }

    @Test
    fun `WHEN a receive tab event occurs with two URLs and a null device THEN the corresponding event is emitted`() {
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast_no_device),
            FxaReceivedTab.Metadata(DeviceType.UNKNOWN, 2)
        )

        val inputTabData = expectedTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        Observable.just(tabReceivedEvent)
            .filterMapToDomainObject()
            .test()
            .assertValues(expected)
    }

    private fun getTwoExpectedTabUrls() = listOf(
        "https://google.com",
        "https://android.com"
    )

    @Test
    fun `WHEN a receive tab event occurs with blank and non-blank URLs THEN an event with tabs with blank URLs filtered out is emitted`() {
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast_no_device),
            FxaReceivedTab.Metadata(DeviceType.UNKNOWN, 2)
        )

        val inputTabUrls = listOf(" ", "") + expectedTabUrls + listOf("  ", "", " ")
        val inputTabData = inputTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        Observable.just(tabReceivedEvent)
            .filterMapToDomainObject()
            .test()
            .assertValues(expected)
    }

    @Test
    fun `WHEN a receive tab event occurs with empty entries THEN sentry records an event and nothing is emitted`() {
        val tabReceivedEvent = ADMIntegration.ReceivedTabs(null, emptyList())

        Observable.just(tabReceivedEvent)
            .filterMapToDomainObject(sentryIntegration)
            .test()
            .assertNoValues()

        verify(exactly = 1) { sentryIntegration.captureAndLogError(any(), any()) }
    }

    @Test
    fun `WHEN a receive tab event occurs with only blank URLs THEN sentry records an event and nothing is emitted`() {
        val inputTabData = List(2) { TabData(title = "TabName $it", url = "   ") }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        Observable.just(tabReceivedEvent)
            .filterMapToDomainObject(sentryIntegration)
            .test()
            .assertNoValues()

        verify(exactly = 1) { sentryIntegration.captureAndLogError(any(), any()) }
    }

    private fun mockADMTabReceivedEvent(
        deviceTypeArg: DeviceType = DeviceType.UNKNOWN,
        deviceName: String = "Name not entered",
        tabDataArg: List<TabData> = emptyList()
    ): ADMIntegration.ReceivedTabs = ADMIntegration.ReceivedTabs(
        device = mockk {
            every { deviceType } returns deviceTypeArg
            every { displayName } returns deviceName
        },
        tabData = tabDataArg
    )
}

fun mockADMTabReceivedEventWithNullDevice(
    tabData: List<TabData>
): ADMIntegration.ReceivedTabs = ADMIntegration.ReceivedTabs(
    device = null,
    tabData = tabData
)
