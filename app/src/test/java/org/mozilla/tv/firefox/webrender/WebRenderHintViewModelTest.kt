/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.hint.Hint
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class WebRenderHintViewModelTest {

    @MockK
    private lateinit var sessionRepo: SessionRepo
    private lateinit var hintVM: WebRenderHintViewModel
    private lateinit var sessionRepoState: Subject<SessionRepo.State>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sessionRepoState = PublishSubject.create()
        every { sessionRepo.state } answers { sessionRepoState }
        hintVM = WebRenderHintViewModel(sessionRepo)
    }

    // TODO tests for isDisplayed will have to wait until behavior is clarified. See
    // https://github.com/mozilla-mobile/firefox-tv/issues/1907#issuecomment-474097863

    @Test
    fun `WHEN always THEN hint should be open menu`() {
        // TODO update this when we have the real strings
        val expectedHints = listOf(Hint(R.string.hint_press_menu_to_open_overlay, R.drawable.hardware_remote_menu))
        val hints = hintVM.hints.test()

        sessionRepoState.onNext(fakeSessionState("https://www.mozilla.org"))
        hintVM.cursorMovedDown()
        sessionRepoState.onNext(fakeSessionState("https://www.google.com"))
        hintVM.cursorReachedBottomOfPage()
        sessionRepoState.onNext(fakeSessionState(URLs.PRIVACY_NOTICE_URL))
        hintVM.cursorReachedTopOfPage()
        hintVM.cursorReachedBottomOfPage()
        sessionRepoState.onNext(fakeSessionState(URLs.APP_URL_POCKET_ERROR))
        hintVM.cursorMovedDown()
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
