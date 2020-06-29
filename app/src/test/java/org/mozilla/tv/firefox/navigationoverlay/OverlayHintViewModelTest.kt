/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.hint.HintContent
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class OverlayHintViewModelTest {

    @MockK private lateinit var sessionRepo: SessionRepo
    @MockK private lateinit var closeMenuHint: HintContent
    private lateinit var hintVM: OverlayHintViewModel
    private lateinit var sessionRepoState: Subject<SessionRepo.State>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sessionRepoState = PublishSubject.create()
        every { sessionRepo.state } answers { sessionRepoState }
        hintVM = OverlayHintViewModel(sessionRepo, closeMenuHint)
    }

    @Test
    fun `hint bar display state should always match back button enabled`() {
        val displayed = hintVM.isDisplayed.test()

        sessionRepoState.onNext(fakeSessionState(backEnabled = true))
        assertEquals(true, displayed.values().last())
        sessionRepoState.onNext(fakeSessionState(backEnabled = false))
        assertEquals(false, displayed.values().last())
    }

    @Test
    fun `WHEN always THEN hint should be close overlay`() {
        val expectedHints = listOf(closeMenuHint)
        val hints = hintVM.hints.test()

        sessionRepoState.onNext(fakeSessionState(url = "https://www.mozilla.org"))
        sessionRepoState.onNext(fakeSessionState(url = "https://www.google.com"))
        sessionRepoState.onNext(fakeSessionState(url = URLs.PRIVACY_NOTICE_URL))
        sessionRepoState.onNext(fakeSessionState(url = URLs.URL_LICENSES))

        hints.values().forEach {
            assertEquals(expectedHints, it)
        }
    }
}

private fun fakeSessionState(url: String = "", backEnabled: Boolean = false) = SessionRepo.State(
        backEnabled,
        false,
        false,
        false,
        url,
        false
)
