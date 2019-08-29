/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.components.concept.sync.Avatar
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.TabData
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ImageSetStrategy
import org.mozilla.tv.firefox.telemetry.SentryIntegration

class FxaRepoTest {

    @MockK(relaxed = true) private lateinit var accountManager: FxaAccountManager
    @MockK(relaxed = true) private lateinit var sentryIntegration: SentryIntegration

    private lateinit var admIntegration: ADMIntegration
    private lateinit var receivedTabsRaw: PublishSubject<ADMIntegration.ReceivedTabs>

    private lateinit var fxaRepo: FxaRepo
    private lateinit var accountState: Observable<FxaRepo.AccountState>
    private lateinit var accountStateTestObs: TestObserver<FxaRepo.AccountState>
    private lateinit var receivedTabsTestObs: TestObserver<ReceivedTabs>

    private val defaultProfileAvatarImage = ImageSetStrategy.ById(R.drawable.ic_default_avatar)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        receivedTabsRaw = PublishSubject.create()
        admIntegration = mockk(relaxed = true) {
            every { receivedTabsRaw } returns this@FxaRepoTest.receivedTabsRaw
        }

        val context = mockk<Context>()
        fxaRepo = FxaRepo(context, accountManager, admIntegration, sentryIntegration)
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
    fun `WHEN on authenticated callback is called with newAccount as false THEN account state is authenticated no profile`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, false)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AuthenticatedNoProfile)
    }

    @Test
    fun `WHEN on authenticated callback is called with newAccount as true THEN account state is authenticated no profile`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, true)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AuthenticatedNoProfile)
    }

    @Test
    fun `WHEN on authenticated callback is called THEN push feature is initialized`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account, true)

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
    fun `GIVEN profile has valid displayName WHEN profile is converted to domain object THEN displayName should be displayName`() {
        val displayName = "displayName"

        val profiles = listOf(
            Profile(null, null, null, displayName),
            Profile("uid", null, null, displayName),
            Profile(null, "email", null, displayName),
            Profile(null, null, Avatar("url", true), displayName),
            Profile("uid", "email", Avatar("url", true), displayName)
        )

        profiles.map { it.toDomainObject() }
            .map { it.displayName }
            .forEach { assertEquals(displayName, it) }
    }

    @Test
    fun `GIVEN profile has no displayName WHEN profile is converted to domain object THEN displayName should be email`() {
        val email = "email"
        val profile = Profile("uid", email, null, null)
        assertEquals(email, profile.toDomainObject().displayName)
    }

    @Test
    fun `GIVEN profile has no displayName or email WHEN profile is converted to domain object THEN displayName should be blank`() {
        val profile = Profile("uid", null, null, null)
        assertEquals("", profile.toDomainObject().displayName)
    }

    @Test
    fun `GIVEN profile has no displayName or email WHEN profile is converted to domain object THEN sentry should log an error`() {
        mockkObject(SentryIntegration)

        Profile("uid", null, null, null).toDomainObject()
        verify(exactly = 1) { SentryIntegration.captureAndLogError(any(), any()) }
    }

    @Test
    fun `GIVEN profile has no avatar WHEN profile is converted to domain object THEN default resource image should be used`() {
        val profile = Profile("uid", null, null, null)

        assertEquals(defaultProfileAvatarImage, profile.toDomainObject().avatarSetStrategy)
    }

    @Test
    fun `GIVEN profile avatar is fxa default WHEN profile is converted to domain object THEN default resource image should be used`() {
        val profiles = listOf(
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", true), null),
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), null),
            Profile(null, null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), null),
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), "display")
        )

        profiles.map { it.toDomainObject() }
            .map { it.avatarSetStrategy }
            .forEach { assertEquals(defaultProfileAvatarImage, it) }
    }

    @Test
    fun `GIVEN profile avatar is nonnull and not default WHEN profile is converted to domain object THEN that url should be used`() {
        val url = "https://www.mozilla.org"
        val expectedStrategy = ImageSetStrategy.ByPath(url, placeholderId = R.drawable.ic_default_avatar, errorId = R.drawable.ic_default_avatar)
        val profiles = listOf(
            Profile("uid", null, Avatar(url, true), null),
            Profile("uid", null, Avatar(url, false), null),
            Profile(null, null, Avatar(url, false), null),
            Profile("uid", null, Avatar(url, false), "display")
        )

        profiles.map { it.toDomainObject() }
            .map { it.avatarSetStrategy }
            .forEach { assertEquals(expectedStrategy, it) }
    }

    @Test
    fun `WHEN one receive tab event occurs with two URLs and a non-null device THEN receivedTabs emits the corresponding event`() {
        val expectedDeviceName = "Expected device name"
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = ReceivedTabs(
            expectedTabUrls,
            sendingDevice = ReceivedTabs.DeviceMetadata(
                expectedDeviceName,
                DeviceType.DESKTOP
        ))

        val inputTabData = expectedTabUrls.mapIndexed { i, url -> TabData("tab title $i", url) }
        val tabReceivedEvent = mockADMTabReceivedEvent(DeviceType.DESKTOP, expectedDeviceName, inputTabData)

        receivedTabsRaw.onNext(tabReceivedEvent)

        receivedTabsTestObs.assertValues(expected)
    }

    @Test
    fun `WHEN a receive tab event occurs with two URLs and a null device THEN receivedTabs emits the corresponding event`() {
        val expectedTabUrls = getTwoExpectedTabUrls()
        val expected = ReceivedTabs(
            expectedTabUrls,
            sendingDevice = null
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
        val expected = ReceivedTabs(
            expectedTabUrls,
            sendingDevice = null
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
