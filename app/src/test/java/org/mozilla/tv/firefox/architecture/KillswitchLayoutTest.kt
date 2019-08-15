/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.architecture

import android.app.Activity
import android.view.View
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.robolectric.Robolectric
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner
import java.util.Locale

@RunWith(FirefoxRobolectricTestRunner::class)
class KillswitchLayoutTest {

    private lateinit var killswitchLayout: KillswitchLayout

    @Before
    fun setup() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        killswitchLayout = KillswitchLayout(activityController.get())
    }

    @Test
    fun `GIVEN requirements are not set WHEN visibility is set THEN visibility should always be gone`() {
        // Don't set requirements
        assertVisibilityIsAlwaysGone()
    }

    @Test
    fun `GIVEN all locales are allowed WHEN visibility is set THEN it should be applied`() {
        killswitchLayout.setRequirements(true, KillswitchLocales.All)

        assertVisibilityIsApplied()
    }

    @Test
    fun `GIVEN experiment is disabled WHEN visibility is set THEN visibility should always be gone`() {
        killswitchLayout.setRequirements(false, KillswitchLocales.All)

        assertVisibilityIsAlwaysGone()
    }

    @Test
    fun `GIVEN visibility is set WHEN requirements are set THEN visibility should match the earlier set values`() {
        killswitchLayout.visibility = View.VISIBLE
        assertEquals(View.GONE, killswitchLayout.visibility)

        killswitchLayout.setRequirements(true, KillswitchLocales.All)
        assertEquals(View.VISIBLE, killswitchLayout.visibility)

        assertVisibilityIsApplied()
    }

    @Test
    fun `GIVEN locale and country are on the accepted list WHEN visibility is set THEN it should be applied`() {
        val mockLocaleManager = mockk<LocaleManager>()
        every { mockLocaleManager.getCurrentLocale(any()) } answers { Locale("es", "mx") }

        killswitchLayout.localeManager = mockLocaleManager

        killswitchLayout.setRequirements(true, KillswitchLocales.ActiveIn(
            Locale("es", "mx"),
            Locale("en", "us")
        ))

        assertVisibilityIsApplied()
    }

    @Test
    fun `GIVEN country is not on the accepted list WHEN visibility is set THEN it should be gone`() {
        val mockLocaleManager = mockk<LocaleManager>()
        every { mockLocaleManager.getCurrentLocale(any()) } answers { Locale("es", "pa") }

        killswitchLayout.localeManager = mockLocaleManager

        killswitchLayout.setRequirements(true, KillswitchLocales.ActiveIn(
            Locale("es", "mx"),
            Locale("en", "us")
        ))

        assertVisibilityIsAlwaysGone()
    }

    @Test
    fun `GIVEN no country is specified AND language is on the list WHEN visibility is set THEN it should be applied`() {
        val mockLocaleManager = mockk<LocaleManager>()
        every { mockLocaleManager.getCurrentLocale(any()) } answers { Locale("es", "pa") }

        killswitchLayout.localeManager = mockLocaleManager

        killswitchLayout.setRequirements(true, KillswitchLocales.ActiveIn(
            Locale("es"),
            Locale("en")
        ))

        assertVisibilityIsApplied()
    }

    private fun assertVisibilityIsAlwaysGone() {
        listOf(
            View.VISIBLE,
            View.INVISIBLE,
            View.GONE,
            View.VISIBLE
        ).forEach {
            killswitchLayout.visibility = it
            assertEquals(View.GONE, killswitchLayout.visibility)
        }
    }

    private fun assertVisibilityIsApplied() {
        killswitchLayout.visibility = View.VISIBLE
        assertEquals(View.VISIBLE, killswitchLayout.visibility)
        killswitchLayout.visibility = View.INVISIBLE
        assertEquals(View.INVISIBLE, killswitchLayout.visibility)
        killswitchLayout.visibility = View.GONE
        assertEquals(View.GONE, killswitchLayout.visibility)
        killswitchLayout.visibility = View.VISIBLE
        assertEquals(View.VISIBLE, killswitchLayout.visibility)
    }
}
