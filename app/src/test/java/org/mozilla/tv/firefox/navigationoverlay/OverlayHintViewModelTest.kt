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
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class OverlayHintViewModelTest {

    @MockK private lateinit var sessionRepo: SessionRepo
    private lateinit var hintVM: OverlayHintViewModel
    private lateinit var sessionRepoState: Subject<SessionRepo.State>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sessionRepoState = PublishSubject.create()
        every { sessionRepo.state } answers { sessionRepoState }
        hintVM = OverlayHintViewModel(sessionRepo)
    }

    @Test
    fun `WHEN current url is home THEN isDisplayed should be false`() {
        val displayed = hintVM.isDisplayed.test()

        sessionRepoState.onNext(fakeSessionState(URLs.APP_URL_HOME))

        assertEquals(false, displayed.values().last())
    }

    @Test
    fun `WHEN current url is not home THEN isDisplayed should be true`() {
        val displayed = hintVM.isDisplayed.test()

        sessionRepoState.onNext(fakeSessionState("https://www.mozilla.org"))
        sessionRepoState.onNext(fakeSessionState("https://www.google.com"))
        sessionRepoState.onNext(fakeSessionState(URLs.PRIVACY_NOTICE_URL))
        sessionRepoState.onNext(fakeSessionState(URLs.APP_URL_POCKET_ERROR))
        sessionRepoState.onNext(fakeSessionState(URLs.URL_LICENSES))

        displayed.values().forEach {
            assertEquals(true, it)
        }
    }

    @Test
    fun `WHEN always THEN hint should be close menu`() {
        // TODO update this when we have the real strings
        val expectedHints = listOf(Hint(R.string.hint_press_menu_to_close_overlay, R.drawable.hardware_remote_menu))
        val hints = hintVM.hints.test()

        sessionRepoState.onNext(fakeSessionState("https://www.mozilla.org"))
        sessionRepoState.onNext(fakeSessionState("https://www.google.com"))
        sessionRepoState.onNext(fakeSessionState(URLs.PRIVACY_NOTICE_URL))
        sessionRepoState.onNext(fakeSessionState(URLs.APP_URL_POCKET_ERROR))
        sessionRepoState.onNext(fakeSessionState(URLs.URL_LICENSES))

        hints.values().forEach {
            assertEquals(expectedHints, it)
        }
    }
}

private fun fakeSessionState(url: String) = SessionRepo.State(
        false,
        false,
        false,
        false,
        url
)
