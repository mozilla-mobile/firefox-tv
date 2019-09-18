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
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.TabData
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.observer.Consumable
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
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
    private lateinit var receivedTabsTestObs: TestObserver<Consumable<FxaReceivedTab>>

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
    fun `WHEN fxa repo is initialized THEN account state is initial`() {
        accountStateTestObs.assertValue(FxaRepo.AccountState.Initial)
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

    @Test
    fun `GIVEN received tabs are not being observed WHEN raw tabs are received THEN they are queued for later use`() {
        fxaRepo.receivedTabs.test().apply {
            assertNoValues()
            dispose()
        }

        receivedTabsRaw.onNext(ADMIntegration.ReceivedTabs(null, listOf(TabData("title", "url"))))

        fxaRepo.receivedTabs.test().assertValueCount(1)
    }

    @Test
    fun `GIVEN received tabs have already been consumed WHEN a new subscriber observes received tabs THEN wrapped value should be null`() {
        receivedTabsRaw.onNext(ADMIntegration.ReceivedTabs(null, listOf(TabData("title", "url"))))

        fxaRepo.receivedTabs.test().apply {
            assertValueCount(1)
            assertValue { it.consume { true } } // Assert there was a value to consume
            dispose()
        }

        fxaRepo.receivedTabs.test().apply {
            assertValueCount(1)
            assertValue { !it.consume { true } } // Assert there was no value to consume
            dispose()
        }
    }
}
