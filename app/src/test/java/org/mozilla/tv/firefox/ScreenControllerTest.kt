/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.view.KeyEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen
import org.mozilla.tv.firefox.helpers.KeyEventHelper
import org.mozilla.tv.firefox.helpers.MockFragmentManagerContainer
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class ScreenControllerTest {

    private lateinit var controller: ScreenController

    private lateinit var fragmentContainer: MockFragmentManagerContainer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        controller = spyk(ScreenController(mockk()))

        fragmentContainer = MockFragmentManagerContainer()
    }

    @Test
    fun `GIVEN any screen is active and handleMenu is a stub returning false WHEN menu is pressed THEN dispatchKeyEvent is not forwarded to any fragments`() {
        // It's bad practice to stub the object under test but we'd have to stub FragmentTransactions, which is impractical.
        every { controller.handleMenu(any()) } returns false

        ActiveScreen.values().forEach { activeScreen ->
            KeyEventHelper.getDownUpKeyEvents(KeyEvent.KEYCODE_MENU).forEach { keyEvent ->
                controller.dispatchKeyEvent(keyEvent, fragmentContainer.fragmentManager, activeScreen)

                verify(exactly = 0) { fragmentContainer.navigationOverlayFragment.dispatchKeyEvent(any()) }
                verify(exactly = 0) { fragmentContainer.webRenderFragment.dispatchKeyEvent(any()) }
            }
        }
    }

    @Test
    fun `GIVEN the WebRenderFragment is active WHEN non-menu KeyEvents are received THEN dispatchKeyEvent is called on the WebRenderFragment`() {
        getNonMenuKeyEvents().forEach { keyEvent ->
            controller.dispatchKeyEvent(keyEvent, fragmentContainer.fragmentManager, ActiveScreen.WEB_RENDER)
            verify { fragmentContainer.webRenderFragment.dispatchKeyEvent(keyEvent) }
        }
    }

    @Test
    fun `GIVEN the NavigationOverlayFragment is active WHEN non-menu KeyEvents are received THEN dispatchKeyEvent is called on the NavigationOverlayFragment`() {
        getNonMenuKeyEvents().forEach { keyEvent ->
            controller.dispatchKeyEvent(keyEvent, fragmentContainer.fragmentManager, ActiveScreen.NAVIGATION_OVERLAY)
            verify { fragmentContainer.navigationOverlayFragment.dispatchKeyEvent(keyEvent) }
        }
    }

    private fun getNonMenuKeyEvents(): List<KeyEvent> = KeyEventHelper.getRandomKeyEventsExcept(KeyEvent.KEYCODE_MENU)
}
