/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.view.KeyEvent
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.KeyEventHelper
import org.mozilla.tv.firefox.telemetry.MenuInteractionMonitor
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class NavigationOverlayFragmentTest {

    private lateinit var navigationOverlayFragment: NavigationOverlayFragment

    @MockK(relaxUnitFun = true) private lateinit var menuInteractionMonitor: MenuInteractionMonitor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        navigationOverlayFragment = NavigationOverlayFragment()
    }

    @Test
    fun `WHEN dispatchKeyEvent is called with a SELECT DOWN event THEN the menuInteractionMonitor is notified`() {
        // This test is tightly coupled to the implementation but that's okay: it's intention is to make sure no one
        // forgets to update the MenuInteractionMonitor if this code changes and I'd rather it break more often than not.
        //
        // The implementation only checks the menu monitor on down events so we only check with those.
        val keyEvents = getDpadSelectDownEvents()
        keyEvents.forEach { navigationOverlayFragment.dispatchKeyEvent(it, menuInteractionMonitor) }
        verify(exactly = keyEvents.size) { menuInteractionMonitor.selectPressed() }
    }

    @Test
    fun `WHEN dispatchKeyEvent is called with a non-SELECT event THEN the menuInteractionMonitor is not notified`() {
        // This test is tightly coupled to the implementation: see test above.
        getNonDpadSelectEvents().forEach { navigationOverlayFragment.dispatchKeyEvent(it, menuInteractionMonitor) }
        verify(exactly = 0) { menuInteractionMonitor.selectPressed() }
    }

    private fun getNonDpadSelectEvents(): List<KeyEvent> = KeyEventHelper.getRandomKeyEventsExcept(listOf(
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER
    ))

    private fun getDpadSelectDownEvents(): List<KeyEvent> = KeyEventHelper.getDownUpKeyEvents(listOf(
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER
    )).filter { it.action == KeyEvent.ACTION_DOWN }
}
