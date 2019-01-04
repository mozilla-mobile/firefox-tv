package org.mozilla.tv.firefox

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.ADD_OVERLAY
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.EXIT_APP
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.NO_OP
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.REMOVE_OVERLAY

class ScreenControllerStateMachineTest {

    var currentUrlIsHome = false
    lateinit var stateMachine: ScreenControllerStateMachine

    @Before
    fun setup() {
        currentUrlIsHome = false
        stateMachine = ScreenControllerStateMachine { currentUrlIsHome }
    }

    @Test
    fun `GIVEN overlay is active and url is home WHEN back is pressed THEN emit exit_app`() {
        stateMachine.currentActiveScreen = NAVIGATION_OVERLAY
        currentUrlIsHome = true
        assertEquals(EXIT_APP, stateMachine.backPress())
    }

    @Test
    fun `GIVEN overlay is active and url is not home WHEN back is pressed THEN emit remove_overlay`() {
        stateMachine.currentActiveScreen = NAVIGATION_OVERLAY
        currentUrlIsHome = false
        assertEquals(REMOVE_OVERLAY, stateMachine.backPress())
    }

    @Test
    fun `GIVEN overlay is active and url is home WHEN menu is pressed THEN emit no_op`() {
        stateMachine.currentActiveScreen = NAVIGATION_OVERLAY
        currentUrlIsHome = true
        assertEquals(NO_OP, stateMachine.menuPress())
    }

    @Test
    fun `GIVEN overlay is active and url is not home WHEN menu is pressed THEN emit remove_overlay`() {
        stateMachine.currentActiveScreen = NAVIGATION_OVERLAY
        currentUrlIsHome = false
        assertEquals(REMOVE_OVERLAY, stateMachine.menuPress())
    }

    @Test
    fun `GIVEN web render is active WHEN back is pressed THEN emit add_overlay`() {
        stateMachine.currentActiveScreen = WEB_RENDER
        assertEquals(ADD_OVERLAY, stateMachine.backPress())
    }

    @Test
    fun `GIVEN web render is active WHEN menu is pressed THEN emit add_overlay`() {
        stateMachine.currentActiveScreen = WEB_RENDER
        assertEquals(ADD_OVERLAY, stateMachine.menuPress())
    }

    // Pocket and Settings fragments will be turned into channels, so they do not have tests
    // TODO remove this comment once channels have been implemented
}
