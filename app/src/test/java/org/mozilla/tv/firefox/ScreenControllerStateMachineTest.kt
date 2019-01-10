package org.mozilla.tv.firefox

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.NAVIGATION_OVERLAY
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.ADD_OVERLAY
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.EXIT_APP
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.NO_OP
import org.mozilla.tv.firefox.ScreenControllerStateMachine.Transition.REMOVE_OVERLAY

class ScreenControllerStateMachineTest {

    @Test
    fun `GIVEN overlay is active and url is home WHEN back is pressed THEN emit exit_app`() {
        val currentActiveScreen = NAVIGATION_OVERLAY
        val currentUrlIsHome = true
        assertEquals(EXIT_APP, ScreenControllerStateMachine.getNewStateBackPress(currentActiveScreen, currentUrlIsHome))
    }

    @Test
    fun `GIVEN overlay is active and url is not home WHEN back is pressed THEN emit remove_overlay`() {
        val currentActiveScreen = NAVIGATION_OVERLAY
        val currentUrlIsHome = false
        assertEquals(REMOVE_OVERLAY, ScreenControllerStateMachine.getNewStateBackPress(currentActiveScreen, currentUrlIsHome))
    }

    @Test
    fun `GIVEN overlay is active and url is home WHEN menu is pressed THEN emit no_op`() {
        val currentActiveScreen = NAVIGATION_OVERLAY
        val currentUrlIsHome = true
        assertEquals(NO_OP, ScreenControllerStateMachine.getNewStateMenuPress(currentActiveScreen, currentUrlIsHome))
    }

    @Test
    fun `GIVEN overlay is active and url is not home WHEN menu is pressed THEN emit remove_overlay`() {
        val currentActiveScreen = NAVIGATION_OVERLAY
        val currentUrlIsHome = false
        assertEquals(REMOVE_OVERLAY, ScreenControllerStateMachine.getNewStateMenuPress(currentActiveScreen, currentUrlIsHome))
    }

    @Test
    fun `GIVEN web render is active WHEN back is pressed THEN emit add_overlay`() {
        val currentActiveScreen = WEB_RENDER
        assertEquals(ADD_OVERLAY, ScreenControllerStateMachine.getNewStateBackPress(currentActiveScreen, false))
    }

    @Test
    fun `GIVEN web render is active WHEN menu is pressed THEN emit add_overlay`() {
        val currentActiveScreen = WEB_RENDER
        assertEquals(ADD_OVERLAY, ScreenControllerStateMachine.getNewStateMenuPress(currentActiveScreen, false))
    }

    // Pocket and Settings fragments will be turned into channels, so they do not have tests
    // TODO remove this comment once channels have been implemented
}
