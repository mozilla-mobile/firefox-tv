package org.mozilla.tv.firefox.fxa

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.TabData
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.framework.UnresolvedString
import java.util.concurrent.TimeUnit

class FxaReceivedTabTest {

    @Test
    fun `WHEN one receive tab event occurs with two URLs and a non-null device THEN receivedTabs emits the corresponding event`() {
        val expectedDeviceName = "Expected device name"
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast, listOf(expectedDeviceName)),
            FxaReceivedTab.Metadata(DeviceType.DESKTOP)
        )

        val inputTabData = expectedTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEvent(DeviceType.DESKTOP, expectedDeviceName, inputTabData)

        receivedTabsRaw.onNext(tabReceivedEvent)

        receivedTabsTestObs.assertValues(expected)
    }

    @Test
    fun `WHEN a receive tab event occurs with two URLs and a null device THEN receivedTabs emits the corresponding event`() {
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast_no_device),
            FxaReceivedTab.Metadata(DeviceType.UNKNOWN)
        )

        val inputTabData = expectedTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        receivedTabsRaw.onNext(tabReceivedEvent)

        receivedTabsTestObs.assertValues(expected)
    }

    private fun getTwoExpectedTabUrls() = listOf(
        "https://google.com",
        "https://android.com"
    )

    @Test
    fun `WHEN a receive tab event occurs with blank and non-blank URLs THEN receivedTabs emits an event with tabs with blank URLs filtered out`() {
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = FxaReceivedTab(
            expectedTabUrls[0],
            UnresolvedString(R.string.fxa_tab_sent_toast_no_device),
            FxaReceivedTab.Metadata(DeviceType.UNKNOWN)
        )

        val inputTabUrls = listOf(" ", "") + expectedTabUrls + listOf("  ", "", " ")
        val inputTabData = inputTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        receivedTabsRaw.onNext(tabReceivedEvent)

        receivedTabsTestObs.assertValues(expected)
    }

    @Test
    fun `WHEN a receive tab event occurs with empty entries THEN sentry records an event and receivedTabs does not emit`() {
        val tabReceivedEvent = ADMIntegration.ReceivedTabs(null, emptyList())

        receivedTabsRaw.onNext(tabReceivedEvent)

        verify(exactly = 1) { sentryIntegration.captureAndLogError(any(), any()) }
        receivedTabsTestObs.assertEmpty()
    }

    @Test
    fun `WHEN a receive tab event occurs with only blank URLs THEN sentry records an event and receivedTabs does not emit`() {
        val inputTabData = List(2) { TabData(title = "TabName $it", url = "   ") }
        val tabReceivedEvent = mockADMTabReceivedEventWithNullDevice(inputTabData)

        receivedTabsRaw.onNext(tabReceivedEvent)

        verify(exactly = 1) { sentryIntegration.captureAndLogError(any(), any()) }
        receivedTabsTestObs.assertEmpty()
    }

    @Test
    fun `WHEN a receive tab event occurs with non-blank URLs THEN telemetry records the event`() {
        val tabEvent = mockADMTabReceivedEventWithNullDevice(listOf(TabData("TabName", url = "https://mozilla.org")))
        receivedTabsRaw.onNext(tabEvent)

        verify(exactly = 1) { telemetryIntegration.receivedTabEvent(any()) }
    }

    @Test
    fun `WHEN fxa state changes THEN telemetry events carrying reauthentication state are sent`() {
        fun waitPastDebounce() {
            FxaRepoTest.testScheduler.advanceTimeBy(11, TimeUnit.SECONDS)
        }

        fxaRepo.accountObserver.onAuthenticated(mockk(relaxed = true), mockk(relaxed = true))
        waitPastDebounce()
        verify(exactly = 1) { telemetryIntegration.doesFxaNeedReauthenticationEvent(false) }

        fxaRepo.accountObserver.onLoggedOut()
        waitPastDebounce()
        verify(exactly = 2) { telemetryIntegration.doesFxaNeedReauthenticationEvent(false) }

        fxaRepo.accountObserver.onProfileUpdated(mockk(relaxed = true))
        waitPastDebounce()
        verify(exactly = 3) { telemetryIntegration.doesFxaNeedReauthenticationEvent(false) }

        fxaRepo.accountObserver.onAuthenticationProblems()
        waitPastDebounce()
        verify(exactly = 1) { telemetryIntegration.doesFxaNeedReauthenticationEvent(true) }
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

    private fun mockADMTabReceivedEventWithNullDevice(
        tabData: List<TabData>
    ): ADMIntegration.ReceivedTabs = ADMIntegration.ReceivedTabs(
        device = null,
        tabData = tabData
    )
}