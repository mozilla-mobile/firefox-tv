/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import android.preference.PreferenceManager
import mozilla.components.browser.session.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.ext.components
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import mozilla.components.browser.engine.system.SystemEngine

@RunWith(RobolectricTestRunner::class)
class TurboModeTest {
    private val context: Context
        get() = RuntimeEnvironment.application

    private lateinit var session: Session

    @Before
    fun setUp() {
        applyUserAgentWorkAround() // TODO remove. See kdoc

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
        context.components.sessionManager.add(session)
    }

    /**
     * A-C v0.26 broke Robolectric tests that instantiate [SystemEngine]s.  A-C is
     * setting [SystemEngine.defaultUserAgent] internally to avoid this problem,
     * but has not yet exposed this functionality.  That will be exposed in the
     * next release, and this workaround will then be deleted.
     *
     * See: https://github.com/mozilla-mobile/android-components/issues/997
     */
    private fun applyUserAgentWorkAround() {
        val defaultUserAgentField = SystemEngine::class.java.getDeclaredField("defaultUserAgent")
        defaultUserAgentField.isAccessible = true
        defaultUserAgentField.set(null, "test-ua-string")
    }

    @Test
    fun `Turbo Mode should be enabled by default`() {
        assertTrue(TurboMode.isEnabled(context))

        assertTrue(Settings.getInstance(context).isBlockingEnabled)
    }

    @Test
    fun `Turbo Mode should be disabled after toggling`() {
        TurboMode.toggle(context, false)

        assertFalse(TurboMode.isEnabled(context))

        assertFalse(Settings.getInstance(context).isBlockingEnabled)
        assertFalse(session.trackerBlockingEnabled)
    }

    @Test
    fun `Turbo Mode should be enabled after toggling again`() {
        TurboMode.toggle(context, false)
        TurboMode.toggle(context, true)

        assertTrue(TurboMode.isEnabled(context))

        assertTrue(Settings.getInstance(context).isBlockingEnabled)
        assertTrue(session.trackerBlockingEnabled)
    }
}
