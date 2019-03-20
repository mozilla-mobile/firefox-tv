/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.view.KeyEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
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
import org.mozilla.tv.firefox.utils.Direction
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class CursorEventRepoTest {

    companion object {
        private lateinit var testScheduler: TestScheduler

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testScheduler = TestScheduler()
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { testScheduler }
            RxJavaPlugins.setInitComputationSchedulerHandler { testScheduler }
        }
    }

    private lateinit var repo: CursorEventRepo
    private lateinit var currentActiveScreen: Subject<ScreenControllerStateMachine.ActiveScreen>
    @MockK private lateinit var cursorController: CursorController
    @MockK private lateinit var screenController: ScreenController

    @Before
    fun setup() {
        testScheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS)
        MockKAnnotations.init(this)
        currentActiveScreen = BehaviorSubject.createDefault(ScreenControllerStateMachine.ActiveScreen.WEB_RENDER)
        every { screenController.currentActiveScreen } answers { currentActiveScreen }
        repo = CursorEventRepo(screenController)
        repo.setCursorController(cursorController)

        every { cursorController.webViewCouldScrollUp() } answers { true }
        every { cursorController.webViewCouldScrollDown() } answers { true }
        every { cursorController.cursorIsNearTopOfScreen() } answers { false }
        every { cursorController.cursorIsNearBottomOfScreen() } answers { false }
    }

    @Test
    fun `WHEN currentActiveScreen is not webRender THEN nothing should be emitted`() {
        val events = repo.webRenderDirectionEvents.test()
        currentActiveScreen.onNext(ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY)
        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        currentActiveScreen.onNext(ScreenControllerStateMachine.ActiveScreen.POCKET)
        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        currentActiveScreen.onNext(ScreenControllerStateMachine.ActiveScreen.SETTINGS)
        pushAndAdvanceTime(KeyEvent.KEYCODE_BUTTON_SELECT)

        assertEquals(0, events.valueCount())
    }

    @Test
    fun `WHEN cursor does not reach top or bottom of screen THEN only movement events should be emitted`() {
        every { cursorController.cursorIsNearTopOfScreen() } answers { false }
        every { cursorController.cursorIsNearBottomOfScreen() } answers { false }

        every { cursorController.webViewCouldScrollUp() } answers { true }
        every { cursorController.webViewCouldScrollDown() } answers { true }

        val events = repo.webRenderDirectionEvents.test()

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        every { cursorController.webViewCouldScrollUp() } answers { false }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        every { cursorController.webViewCouldScrollUp() } answers { true }
        every { cursorController.webViewCouldScrollDown() } answers { false }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        assertTrue(events.values().none { it is CursorEventRepo.CursorEvent.ScrolledToEdge })
    }

    @Test
    fun `GIVEN webpage cannot scroll up or down WHEN cursor reaches top or bottom of screen THEN scroll events should be emitted`() {
        every { cursorController.webViewCouldScrollUp() } answers { false }
        every { cursorController.webViewCouldScrollDown() } answers { false }

        val events = repo.webRenderDirectionEvents.test()

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)

        every { cursorController.cursorIsNearTopOfScreen() } answers { true }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_UP)
        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        every { cursorController.cursorIsNearTopOfScreen() } answers { false }
        every { cursorController.cursorIsNearBottomOfScreen() } answers { true }

        pushAndAdvanceTime(KeyEvent.KEYCODE_DPAD_DOWN)

        println(events.values())

        assertTrue(events.values().any { it == CursorEventRepo.CursorEvent.ScrolledToEdge(Direction.UP) })
        assertTrue(events.values().any { it == CursorEventRepo.CursorEvent.ScrolledToEdge(Direction.DOWN) })
    }

    private fun fakeKeyEvent(key: Int): KeyEvent = KeyEvent(KeyEvent.ACTION_DOWN, key)

    private fun pushAndAdvanceTime(key: Int) {
        testScheduler.advanceTimeBy(11, TimeUnit.MILLISECONDS)
        repo.pushKeyEvent(fakeKeyEvent(key))
    }
}
