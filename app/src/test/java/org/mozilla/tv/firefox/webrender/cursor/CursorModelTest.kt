/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.view.KeyEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.helpers.RxTestHelper
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(FirefoxRobolectricTestRunner::class)
class CursorModelTest {

    companion object {
        private lateinit var testScheduler: TestScheduler

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testScheduler = RxTestHelper.forceRxTestSchedulerInBeforeClass()
        }
    }

    private lateinit var currentActiveScreen: Subject<ScreenControllerStateMachine.ActiveScreen>
    @MockK private lateinit var cursorModel: CursorModel
    @MockK private lateinit var screenController: ScreenController
    @MockK private lateinit var frameworkRepo: FrameworkRepo
    @MockK private lateinit var sessionRepo: SessionRepo

    @Before
    fun setup() {
        testScheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS)
        MockKAnnotations.init(this)
        currentActiveScreen = BehaviorSubject.createDefault(ScreenControllerStateMachine.ActiveScreen.WEB_RENDER)
        every { screenController.currentActiveScreen } answers { currentActiveScreen }
        every { frameworkRepo.isVoiceViewEnabled } answers { Observable.just(true) }
        every { sessionRepo.state } answers { Observable.just(SessionRepo.State(
                backEnabled = true,
                forwardEnabled = true,
                desktopModeActive = false,
                turboModeActive = false,
                currentUrl = "https://www.mozilla.com",
                loading = false
        )) }
        cursorModel = CursorModel(currentActiveScreen, frameworkRepo, sessionRepo)

        cursorModel.webViewCouldScrollInDirectionProvider = { true }
    }

    @Test
    fun `WHEN currentActiveScreen is not webRender THEN nothing should be emitted`() {
        val events = cursorModel.cursorMovedEvents.test()
        currentActiveScreen.onNext(ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY)
        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        currentActiveScreen.onNext(ScreenControllerStateMachine.ActiveScreen.SETTINGS)
        pushAndAdvanceTime(KeyEvent.KEYCODE_BUTTON_SELECT)

        assertEquals(0, events.valueCount())
    }

    @Test
    fun `WHEN cursor does not reach top or bottom of screen THEN only movement events should be emitted`() {
        val events = cursorModel.cursorMovedEvents.test()

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        cursorModel.webViewCouldScrollInDirectionProvider = { it == Direction.UP }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        cursorModel.webViewCouldScrollInDirectionProvider = { it == Direction.UP }
        cursorModel.webViewCouldScrollInDirectionProvider = { it == Direction.DOWN }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        assertTrue(events.values().none { it is CursorEvent.ScrolledToEdge })
    }

    private fun fakeKeyEvent(key: Int): KeyEvent = KeyEvent(KeyEvent.ACTION_DOWN, key)

    private fun pushAndAdvanceTime(key: Int) {
        testScheduler.advanceTimeBy(11, TimeUnit.MILLISECONDS)
        cursorModel.handleKeyEvent(fakeKeyEvent(key))
    }
}
