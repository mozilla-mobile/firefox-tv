/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.robolectric.RobolectricTestRunner

/** A list of test URLs that span different "categories".  */
private val POSSIBLE_URLS = listOf(
    // Non-URLs
    "",
    " ",
    "http",
    "mozilla.org",

    // HTTP vs. HTTPS
    "http://mozilla.org",
    "https://mozilla.org",

    // Youtube sites vs non-Youtube sites.
    "https://youtube.com/tv",
    "https://tv.youtube.com",
    "https://youtube.com",
    "https://m.youtube.com"
)

@RunWith(RobolectricTestRunner::class) // Required to mock MotionEvent.obtain() tests
class CursorViewModelTest {

    private lateinit var viewModel: CursorViewModel
    private lateinit var isEnabledTestObs: TestObserver<Boolean>

    private lateinit var frameworkRepo: FrameworkRepo
    private lateinit var isVoiceViewEnabled: BehaviorSubject<Boolean>

    private lateinit var screenController: ScreenController
    private lateinit var activeScreen: Subject<ActiveScreen>

    private lateinit var sessionRepo: SessionRepo
    private lateinit var sessionState: BehaviorSubject<SessionRepo.State>

    @Before
    fun setUp() {
        frameworkRepo = mock(FrameworkRepo::class.java).also {
            isVoiceViewEnabled = BehaviorSubject.create()
            `when`(it.isVoiceViewEnabled).thenReturn(isVoiceViewEnabled)
        }

        screenController = mock(ScreenController::class.java).also {
            activeScreen = PublishSubject.create()
            `when`(it.currentActiveScreen).thenReturn(activeScreen)
        }

        sessionRepo = mock(SessionRepo::class.java).also {
            sessionState = BehaviorSubject.create()
            `when`(it.state).thenReturn(sessionState)
        }

        viewModel = CursorViewModel(frameworkRepo, screenController, sessionRepo).also {
            isEnabledTestObs = it.isEnabled.test()
        }
    }

    // This method pattern is duplicated but it's significantly more readable this way.
    @Test
    fun `GIVEN VoiceView is enabled THEN the cursor is disabled`() {
        val (initialScreen, restScreens) = firstAndRestScreens()
        val (initialUrl, restUrls) = POSSIBLE_URLS.firstAndRest()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + restScreens.size + restUrls.size

        isVoiceViewEnabled.onNext(true)

        activeScreen.onNext(initialScreen)
        sessionState.onNext(newStateWithUrl(initialUrl))

        // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
        restScreens.forEach { activeScreen.onNext(it) }
        restUrls.forEach { sessionState.onNext(newStateWithUrl(it)) }

        isEnabledTestObs.assertValues(*Array(emittedEventCount) { false })
    }

    @Test
    fun `GIVEN url is youtube TV THEN cursor is disabled`() {
        val (initialScreen, restScreens) = firstAndRestScreens()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + 1 /* isVoiceViewEnabled bool */ + restScreens.size

        sessionState.onNext(newStateWithUrl("https://youtube.com/tv/"))

        activeScreen.onNext(initialScreen)
        isVoiceViewEnabled.onNext(false)

        // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
        restScreens.forEach { activeScreen.onNext(it) }
        isVoiceViewEnabled.onNext(true)

        isEnabledTestObs.assertValues(*Array(emittedEventCount) { false })
    }

    @Test
    fun `GIVEN web render fragment is not visible THEN cursor is disabled`() {
        val (initialScreen, restScreens) = ActiveScreen.values()
            .filter { it != WEB_RENDER } // web render is not visible.
            .firstAndRest()
        val (initialUrl, restUrls) = firstAndRestUrls()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + 1 /* isVoiceViewEnabled bool */ + restUrls.size + restScreens.size

        isVoiceViewEnabled.onNext(false)
        sessionState.onNext(newStateWithUrl(initialUrl))
        activeScreen.onNext(initialScreen)

        // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
        restScreens.forEach { activeScreen.onNext(it) }
        restUrls.forEach { sessionState.onNext(newStateWithUrl(it)) }
        isVoiceViewEnabled.onNext(true)

        isEnabledTestObs.assertValues(*Array(emittedEventCount) { false })
    }

    @Test
    fun `GIVEN VoiceView is disabled, web fragment is visible, and url is not youtube TV THEN cursor is enabled`() {
        val (initialUrl, restUrls) = POSSIBLE_URLS
            .filter { !it.contains("youtube.com/tv") } // url is not youtube TV: good enough filter for now.
            .firstAndRest()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + restUrls.size

        isVoiceViewEnabled.onNext(false)
        activeScreen.onNext(WEB_RENDER)
        sessionState.onNext(newStateWithUrl(initialUrl))

        // Now that initial value is set, iterate over remaining states.
        restUrls.forEach { sessionState.onNext(newStateWithUrl(it)) }

        isEnabledTestObs.assertValues(*Array(emittedEventCount) { true })
    }

    private fun newStateWithUrl(url: String): SessionRepo.State {
        return SessionRepo.State(false, false, false, false, url, false)
    }

    private fun firstAndRestScreens(): Pair<ActiveScreen, List<ActiveScreen>> {
        return ActiveScreen.values().toList().firstAndRest()
    }

    private fun firstAndRestUrls(): Pair<String, List<String>> {
        return POSSIBLE_URLS.firstAndRest()
    }
}

private fun <T> List<T>.firstAndRest(): Pair<T, List<T>> {
    return Pair(this.first(), drop(1))
}
