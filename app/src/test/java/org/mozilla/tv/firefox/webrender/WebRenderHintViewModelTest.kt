/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.hint.HintContent
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.webrender.cursor.CursorEvent
import org.mozilla.tv.firefox.webrender.cursor.CursorModel

class WebRenderHintViewModelTest {

    @MockK private lateinit var sessionRepo: SessionRepo
    @MockK private lateinit var screenController: ScreenController
    @MockK private lateinit var cursorModel: CursorModel
    @MockK private lateinit var openMenuHint: HintContent
    private lateinit var hintVM: WebRenderHintViewModel
    private lateinit var sessionRepoState: Subject<SessionRepo.State>
    private lateinit var webRenderDirectionEvents: Subject<CursorEvent>
    private lateinit var currentActiveScreen: Subject<ActiveScreen>
    private lateinit var displayed: TestObserver<Boolean>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sessionRepoState = PublishSubject.create()
        every { sessionRepo.state } answers { sessionRepoState }
        webRenderDirectionEvents = PublishSubject.create()
        every { cursorModel.cursorMovedEvents } answers { webRenderDirectionEvents }
        currentActiveScreen = PublishSubject.create()
        every { screenController.currentActiveScreen } answers { currentActiveScreen }

        hintVM = WebRenderHintViewModel(sessionRepo, cursorModel, screenController, openMenuHint)
        displayed = hintVM.isDisplayed.test()

        sessionRepoState.onNext(fakeSessionState())
    }

    @Test
    fun `GIVEN current url is not a YouTube TV video WHEN always THEN hint should be open menu`() {
        val expectedHints = listOf(openMenuHint)
        val hints = hintVM.hints.test()

        sessionRepoState.onNext(fakeSessionState("https://www.mozilla.org"))
        pushScrolledToEdge(Direction.DOWN)
        sessionRepoState.onNext(fakeSessionState("https://www.google.com"))
        pushCursorMove(Direction.UP)
        sessionRepoState.onNext(fakeSessionState(URLs.PRIVACY_NOTICE_URL))
        pushCursorMove(Direction.LEFT)
        pushScrolledToEdge(Direction.UP)
        pushCursorMove(Direction.LEFT)
        sessionRepoState.onNext(fakeSessionState(URLs.URL_LICENSES))

        hints.values().forEach {
            assertEquals(expectedHints, it)
        }
    }

    @Test
    fun `GIVEN current url is not a YouTube TV video WHEN active screen changes to web render THEN hints should be displayed`() {

        currentActiveScreen.onNext(ActiveScreen.SETTINGS)
        assertEquals(0, displayed.valueCount())

        currentActiveScreen.onNext(ActiveScreen.WEB_RENDER)
        assertEquals(1, displayed.valueCount())
        assertEquals(true, displayed.values().last())

        currentActiveScreen.onNext(ActiveScreen.SETTINGS)
        assertEquals(1, displayed.valueCount())
    }

    @Test
    fun `GIVEN current url is not a YouTube TV video WHEN cursor up and down events are received THEN hints should be hidden`() {

        pushCursorMove(Direction.UP)
        assertEquals(1, displayed.valueCount())
        assertEquals(false, displayed.values().last())

        pushCursorMove(Direction.LEFT)
        assertEquals(1, displayed.valueCount())

        pushScrolledToEdge(Direction.DOWN)
        assertEquals(2, displayed.valueCount())
        assertEquals(true, displayed.values().last())

        pushCursorMove(Direction.DOWN)
        assertEquals(3, displayed.valueCount())
        assertEquals(false, displayed.values().last())

        pushCursorMove(Direction.RIGHT)
        assertEquals(3, displayed.valueCount())
    }

    @Test
    fun `GIVEN current url is not a YouTube TV video WHEN cursor scrolls past top or bottom of page THEN hints should be displayed`() {

        pushScrolledToEdge(Direction.UP)
        assertEquals(1, displayed.valueCount())
        assertEquals(true, displayed.values().last())

        pushScrolledToEdge(Direction.LEFT)
        assertEquals(1, displayed.valueCount())

        pushCursorMove(Direction.DOWN)
        assertEquals(2, displayed.valueCount())
        assertEquals(false, displayed.values().last())

        pushScrolledToEdge(Direction.DOWN)
        assertEquals(3, displayed.valueCount())
        assertEquals(true, displayed.values().last())

        pushScrolledToEdge(Direction.RIGHT)
        assertEquals(3, displayed.valueCount())
    }

    @Test
    fun `GIVEN current url is not a YouTube TV video WHEN loading completes THEN hints should be displayed`() {

        sessionRepoState.onNext(fakeSessionState(loading = true))
        assertEquals(0, displayed.valueCount())

        sessionRepoState.onNext(fakeSessionState(loading = false))
        assertEquals(1, displayed.valueCount())
        assertEquals(true, displayed.values().last())

        sessionRepoState.onNext(fakeSessionState(loading = true))
        assertEquals(1, displayed.valueCount())
    }

    @Test
    fun `WHEN current url is a YouTube TV video THEN hints should not be displayed`() {
        val youTubeVideo = "https://www.youtube.com/tv#/watch/video/control?v=TgWKutWMT-o&resume"

        fun assertDisplayedIsFalse() {
            assertEquals(false, displayed.values().last())
        }

        sessionRepoState.onNext(fakeSessionState(url = youTubeVideo))
        assertDisplayedIsFalse()

        pushCursorMove(Direction.UP)
        assertDisplayedIsFalse()

        pushCursorMove(Direction.LEFT)
        assertDisplayedIsFalse()

        pushScrolledToEdge(Direction.DOWN)
        assertDisplayedIsFalse()

        pushScrolledToEdge(Direction.RIGHT)
        assertDisplayedIsFalse()

        sessionRepoState.onNext(fakeSessionState(url = youTubeVideo, loading = true))
        assertDisplayedIsFalse()

        sessionRepoState.onNext(fakeSessionState(url = youTubeVideo, loading = false))
        assertDisplayedIsFalse()
    }

    fun pushScrolledToEdge(direction: Direction) {
        webRenderDirectionEvents.onNext(CursorEvent.ScrolledToEdge(direction))
    }

    fun pushCursorMove(direction: Direction) {
        webRenderDirectionEvents.onNext(CursorEvent.CursorMoved(direction))
    }
}

private fun fakeSessionState(url: String = "", loading: Boolean = false) = SessionRepo.State(
        false,
        false,
        false,
        false,
        url,
        loading
)
