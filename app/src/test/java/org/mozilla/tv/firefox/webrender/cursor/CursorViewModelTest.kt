/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.graphics.PointF
import android.view.KeyEvent
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.helpers.ext.assertValues
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule
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

    @get:Rule
    val preventLiveDataMainLooperCrashRule = PreventLiveDataMainLooperCrashRule()

    private lateinit var viewModel: CursorViewModel

    private lateinit var frameworkRepo: FrameworkRepo
    private lateinit var isVoiceViewEnabled: MutableLiveData<Boolean>

    private lateinit var screenController: ScreenController
    private lateinit var activeScreen: Subject<ActiveScreen>

    private lateinit var sessionRepo: SessionRepo
    private lateinit var sessionState: MutableLiveData<SessionRepo.State>

    @Before
    fun setUp() {
        frameworkRepo = mock(FrameworkRepo::class.java).also {
            isVoiceViewEnabled = MutableLiveData()
            `when`(it.isVoiceViewEnabled).thenReturn(isVoiceViewEnabled)
        }

        screenController = mock(ScreenController::class.java).also {
            activeScreen = PublishSubject.create()
            `when`(it.currentActiveScreen).thenReturn(activeScreen)
        }

        sessionRepo = mock(SessionRepo::class.java).also {
            sessionState = MutableLiveData()
            @Suppress("DEPRECATION")
            `when`(it.legacyState).thenReturn(sessionState)
        }

        viewModel = CursorViewModel(frameworkRepo, screenController, sessionRepo)
    }

    // This method pattern is duplicated but it's significantly more readable this way.
    @Test
    fun `GIVEN VoiceView is enabled THEN the cursor is disabled`() {
        val (initialScreen, restScreens) = firstAndRestScreens()
        val (initialUrl, restUrls) = POSSIBLE_URLS.firstAndRest()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + restScreens.size + restUrls.size

        viewModel.isEnabled.assertValueForEmissionCount(false, emittedEventCount) {
            isVoiceViewEnabled.value = true

            activeScreen.onNext(initialScreen)
            sessionState.value = newStateWithUrl(initialUrl)

            // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
            restScreens.forEach { activeScreen.onNext(it) }
            restUrls.forEach { sessionState.value = newStateWithUrl(it) }
        }
    }

    @Test
    fun `GIVEN url is youtube TV THEN cursor is disabled`() {
        val (initialScreen, restScreens) = firstAndRestScreens()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + 1 /* isVoiceViewEnabled bool */ + restScreens.size

        viewModel.isEnabled.assertValueForEmissionCount(false, emittedEventCount) {
            sessionState.value = newStateWithUrl("https://youtube.com/tv/")

            activeScreen.onNext(initialScreen)
            isVoiceViewEnabled.value = false

            // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
            restScreens.forEach { activeScreen.onNext(it) }
            isVoiceViewEnabled.value = true
        }
    }

    @Test
    fun `GIVEN web render fragment is not visible THEN cursor is disabled`() {
        val (initialScreen, restScreens) = ActiveScreen.values()
            .filter { it != WEB_RENDER } // web render is not visible.
            .firstAndRest()
        val (initialUrl, restUrls) = firstAndRestUrls()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + 1 /* isVoiceViewEnabled bool */ + restUrls.size + restScreens.size

        viewModel.isEnabled.assertValueForEmissionCount(false, emittedEventCount) {
            isVoiceViewEnabled.value = false
            sessionState.value = newStateWithUrl(initialUrl)
            activeScreen.onNext(initialScreen)

            // Now that initial value is set, iterate over remaining states. Ideally this would be all permutations.
            restScreens.forEach { activeScreen.onNext(it) }
            restUrls.forEach { sessionState.value = newStateWithUrl(it) }
            isVoiceViewEnabled.value = true
        }
    }

    @Test
    fun `GIVEN VoiceView is disabled, web fragment is visible, and url is not youtube TV THEN cursor is enabled`() {
        val (initialUrl, restUrls) = POSSIBLE_URLS
            .filter { !it.contains("youtube.com/tv") } // url is not youtube TV: good enough filter for now.
            .firstAndRest()

        // 1 event is emitted across all initial values: no events are emitted until all initial values are set.
        val emittedEventCount = 1 + restUrls.size

        viewModel.isEnabled.assertValueForEmissionCount(true, emittedEventCount) {
            isVoiceViewEnabled.value = false
            activeScreen.onNext(WEB_RENDER)
            sessionState.value = newStateWithUrl(initialUrl)

            // Now that initial value is set, iterate over remaining states.
            restUrls.forEach { sessionState.value = newStateWithUrl(it) }
        }
    }

    @Test
    fun `GIVEN intended Click event THEN ACTION_UP MotionEvent returns same event_time as ACTION_DOWN`() {
        val pos = PointF(0f, 0f)
        val downTime = 1000L
        val upTime = downTime + DPAD_TAP_TIMEOUT - 1

        val downEvent = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, downTime, pos)

        assertNotNull(downEvent)
        assertEquals(downEvent!!.eventTime, downTime)

        val upEvent = viewModel.validateMotionEvent(KeyEvent.ACTION_UP, upTime, pos)

        assertNotNull(upEvent)
        assertEquals(upEvent!!.eventTime, downEvent.eventTime)
    }

    @Test
    fun `GIVEN intended LongPress event THEN ACTION_UP MotionEvent returns different event_time as ACTION_DOWN`() {
        val pos = PointF(0f, 0f)
        val downTimeInitial = 1000L
        val upTime = downTimeInitial + DPAD_LONG_PRESS_TIMEOUT - 1

        val downEvent = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, downTimeInitial, pos)

        assertNotNull(downEvent)
        assertEquals(downEvent!!.eventTime, downTimeInitial)

        // Simulate spammed consecutive down events
        for (i in 1..5) {
            val currDownTime = downTimeInitial + i
            val consecutiveDown = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, currDownTime, pos)
            assertNull(consecutiveDown)
        }

        val upEvent = viewModel.validateMotionEvent(KeyEvent.ACTION_UP, upTime, pos)

        assertNotNull(upEvent)
        assertNotEquals(upEvent!!.eventTime, downEvent.eventTime)
        assertEquals(upEvent.eventTime, upTime)
    }

    @Test
    fun `GIVEN down event exceeding LongPressTimeout THEN MotionEvents reset and register new down event`() {
        val pos = PointF(0f, 0f)
        val downTimeInitial = 1000L

        val downEvent = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, downTimeInitial, pos)

        assertNotNull(downEvent)
        assertEquals(downEvent!!.eventTime, downTimeInitial)

        // Simulate spammed consecutive down events
        for (i in 1..5) {
            val currDownTime = downTimeInitial + i
            val consecutiveDown = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, currDownTime, pos)
            assertNull(consecutiveDown)
        }

        // Down event comes when neither a long press or tab => everything should reset
        val downTimeNew = downTimeInitial + DPAD_LONG_PRESS_TIMEOUT + 1
        val downEventNew = viewModel.validateMotionEvent(KeyEvent.ACTION_DOWN, downTimeNew, pos)

        assertNotNull(downEventNew)
        assertEquals(downEventNew!!.eventTime, downTimeNew)
    }

    private fun newStateWithUrl(url: String): SessionRepo.State {
        return SessionRepo.State(false, false, false, false, url)
    }

    private fun firstAndRestScreens(): Pair<ActiveScreen, List<ActiveScreen>> {
        return ActiveScreen.values().toList().firstAndRest()
    }

    private fun firstAndRestUrls(): Pair<String, List<String>> {
        return POSSIBLE_URLS.firstAndRest()
    }

    private fun LiveData<Boolean>.assertValueForEmissionCount(expected: Boolean, emissionCount: Int, pushValues: () -> Unit) {
        val expectedEmissions = Array(emissionCount) { expected }
        assertValues(*expectedEmissions) { pushValues() }
    }
}

private fun <T> List<T>.firstAndRest(): Pair<T, List<T>> {
    return Pair(this.first(), drop(1))
}
