/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.TabData
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.framework.UnresolvedString
import org.mozilla.tv.firefox.helpers.RxTestHelper
import org.mozilla.tv.firefox.telemetry.SentryIntegration
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.util.concurrent.TimeUnit

class FxaRepoTest {

    companion object {
        private lateinit var testScheduler: TestScheduler

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testScheduler = RxTestHelper.forceRxTestSchedulerInBeforeClass()
        }
    }

    @MockK(relaxed = true) private lateinit var accountManager: FxaAccountManager
    @MockK(relaxed = true) private lateinit var telemetryIntegration: TelemetryIntegration
    @MockK(relaxed = true) private lateinit var sentryIntegration: SentryIntegration

    private lateinit var admIntegration: ADMIntegration
    private lateinit var receivedTabsRaw: PublishSubject<ADMIntegration.ReceivedTabs>

    private lateinit var fxaRepo: FxaRepo
    private lateinit var accountState: Observable<FxaRepo.AccountState>
    private lateinit var accountStateTestObs: TestObserver<FxaRepo.AccountState>
    private lateinit var receivedTabsTestObs: TestObserver<FxaReceivedTab>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        receivedTabsRaw = PublishSubject.create()
        admIntegration = mockk(relaxed = true) {
            every { receivedTabsRaw } returns this@FxaRepoTest.receivedTabsRaw
        }

        val context = mockk<Context>()
        fxaRepo = FxaRepo(context, accountManager, admIntegration, telemetryIntegration, sentryIntegration)
        accountState = fxaRepo.accountState
        accountStateTestObs = accountState.test()

        receivedTabsTestObs = fxaRepo.receivedTabs.test()
    }

    @Test
    fun `WHEN fxa repo is initialized THEN the account observer is registered once`() {
        verify(exactly = 1) { accountManager.register(fxaRepo.accountObserver) }
    }

    @Test
    fun `WHEN fxa repo is initialized THEN account state is not authenticated`() {
        accountStateTestObs.assertValue(FxaRepo.AccountState.NotAuthenticated)
    }

    @Test
    fun `WHEN on authenticated callback is called with authType as Signin THEN account state is authenticated no profile`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, AuthType.Signin)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AuthenticatedNoProfile)
    }

    @Test
    fun `WHEN on authenticated callback is called with authType as Signup THEN account state is authenticated no profile`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, AuthType.Signup)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AuthenticatedNoProfile)
    }

    @Test
    fun `WHEN on authenticated callback is called THEN push feature is initialized`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, AuthType.Signin)

        verify(exactly = 1) { admIntegration.initPushFeature() }
    }

    @Test
    fun `WHEN on authentication problems callback is called THEN account state is needs reauthentication`() {
        fxaRepo.accountObserver.onAuthenticationProblems()
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.NeedsReauthentication)
    }

    @Test
    fun `WHEN on logout callback is called THEN account state is not authenticated`() {
        fxaRepo.accountObserver.onLoggedOut()
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.NotAuthenticated)
    }

    @Test
    fun `WHEN on logout callback is called THEN push feature is shutdown`() {
        fxaRepo.accountObserver.onLoggedOut()

        verify(exactly = 1) { admIntegration.shutdownPushFeature() }
    }

    @Test
    fun `WHEN on profile update callback is called THEN account state is authenticated with profile`() {
        val profile = Profile("uid", "email", null, "displayName")
        fxaRepo.accountObserver.onProfileUpdated(profile)
        accountStateTestObs.assertValueAt(1) { it is FxaRepo.AccountState.AuthenticatedWithProfile }
    }

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
            testScheduler.advanceTimeBy(11, TimeUnit.SECONDS)
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
