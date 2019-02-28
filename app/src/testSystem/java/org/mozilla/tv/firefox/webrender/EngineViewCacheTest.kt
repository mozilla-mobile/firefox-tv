/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.browser.engine.system.SystemEngineView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.session.SessionRepo
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineViewCacheTest {

    private lateinit var engineViewCache: EngineViewCache

    @Before
    fun setup() {
        // A fake user agent is required in order to instantiate WebRenderComponents.engine in a test
        SystemEngine.defaultUserAgent = "test-ua-string"
        engineViewCache = EngineViewCache(mock(SessionRepo::class.java))
    }

    @Test
    fun `WHEN getEngineView is called multiple times THEN the same EngineView is returned`() {
        val engineView = getEngineView()
        val message = "Expected saved engineView to equal retrieved EngineView"
        assertTrue(message, engineView === getEngineView())
        assertTrue(message, engineView === getEngineView())
        assertTrue(message, engineView === getEngineView())
    }

    @Test
    fun `GIVEN cached EngineView has a parent WHEN EngineView is returned THEN EngineView should be removed from parent`() {
        // Setup
        val engineView = getEngineView()
        assertEquals(null, engineView.parent)
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())
        parent.addView(engineView)
        assertEquals(parent, engineView.parent)

        // Test
        assertEquals(null, getEngineView().parent)
    }

    private fun getEngineView(): SystemEngineView {
        return engineViewCache.getEngineView(ApplicationProvider.getApplicationContext(),
                Robolectric.buildAttributeSet().build()) {}
    }
}
