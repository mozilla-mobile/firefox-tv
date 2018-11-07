/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.session.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.robolectric.RobolectricTestRunner
import mozilla.components.browser.engine.system.SystemEngine
import org.mozilla.tv.firefox.ext.application

@RunWith(RobolectricTestRunner::class)
class ProdTurboModeTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var session: Session
    private lateinit var turboMode: TurboMode

    @Before
    fun setUp() {
        SystemEngine.defaultUserAgent = "test-ua-string"

        // Avoid [Settings] from keeping a references to a shared preference instance from a previous test run.
        Settings.reset()

        // Clear all settings to always start fresh
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .apply()

        // Add a session so that we can verify the state of it
        session = Session("about:blank")
        context.webRenderComponents.sessionManager.add(session)
        turboMode = ProdTurboMode(context.application)
    }

    @Test
    fun `Turbo Mode should be enabled by default`() {
        assertTrue(turboMode.isEnabled())

        assertTrue(Settings.getInstance(context).isBlockingEnabled)
    }

    @Test
    fun `Turbo Mode should be disabled after toggling`() {
        turboMode.setEnabled(false)

        assertFalse(turboMode.isEnabled())

        assertFalse(Settings.getInstance(context).isBlockingEnabled)
        assertFalse(session.trackerBlockingEnabled)
    }

    @Test
    fun `Turbo Mode should be enabled after toggling again`() {
        turboMode.setEnabled(false)
        turboMode.setEnabled(true)

        assertTrue(turboMode.isEnabled())

        assertTrue(Settings.getInstance(context).isBlockingEnabled)
        assertTrue(session.trackerBlockingEnabled)
    }
}
