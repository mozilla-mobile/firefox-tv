/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Before
import org.junit.Test

class FxaRepoTest {

    @MockK(relaxed = true) private lateinit var accountManager: FxaAccountManager

    private lateinit var fxaRepo: FxaRepo
    private lateinit var accountState: BehaviorSubject<FxaRepo.AccountState>
    private lateinit var accountStateTestObs: TestObserver<FxaRepo.AccountState>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val context = mockk<Context>()
        fxaRepo = FxaRepo(context, accountManager)
        accountState = fxaRepo.accountState
        accountStateTestObs = accountState.test()
    }

    @Test
    fun `WHEN fxa repo is initialized THEN the account observer is registered once`() {
        verify(exactly = 1) { accountManager.register(fxaRepo.accountObserver) }
    }

    @Test
    fun `WHEN fxa repo is initialized THEN account state is not authenticated`() {
        accountStateTestObs.assertValue(FxaRepo.AccountState.NOT_AUTHENTICATED)
    }

    @Test
    fun `WHEN on authenticated callback is called THEN account state is authenticated no profile`() {
        val account = mockk<OAuthAccount>()
        fxaRepo.accountObserver.onAuthenticated(account)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AUTHENTICATED_NO_PROFILE)
    }

    @Test
    fun `WHEN on authentication problems callback is called THEN account state is needs reauthentication`() {
        fxaRepo.accountObserver.onAuthenticationProblems()
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.NEEDS_REAUTHENTICATION)
    }

    @Test
    fun `WHEN on logout callback is called THEN account state is not authenticated`() {
        fxaRepo.accountObserver.onLoggedOut()
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.NOT_AUTHENTICATED)
    }

    @Test
    fun `WHEN on profile update callback is called THEN account state is authenticated with profile`() {
        val profile = mockk<Profile>()
        fxaRepo.accountObserver.onProfileUpdated(profile)
        accountStateTestObs.assertValueAt(1, FxaRepo.AccountState.AUTHENTICATED_WITH_PROFILE)
    }
}