/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.framework

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameworkRepoTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var repo: FrameworkRepo

    @Before
    fun setUp() {
        repo = FrameworkRepo()
    }

    @Test
    fun `WHEN init is called THEN a touch exploration state change listener is added`() {
        val a11yManager = mock(AccessibilityManager::class.java)
        repo.init(a11yManager)
        verify(a11yManager, times(1)).addTouchExplorationStateChangeListener(any(TouchExplorationStateChangeListener::class.java))
    }

    @Test
    fun `WHEN init is called THEN an initial touch exploration state is set from the accessibility manager`() {
        val a11yManager = mock(AccessibilityManager::class.java)
        `when`(a11yManager.isTouchExplorationEnabled).thenReturn(true)
        repo.init(a11yManager)
        assertTrue(repo.isVoiceViewEnabled.value!!)

        `when`(a11yManager.isTouchExplorationEnabled).thenReturn(false)
        repo.init(a11yManager)
        assertFalse(repo.isVoiceViewEnabled.value!!)
    }

    @Test
    fun `WHEN touch exploration state changes to false THEN voice view is disabled`() {
        repo.touchExplorationStateChangeListener.onTouchExplorationStateChanged(false)
        repo.isVoiceViewEnabled.observeForever { assertFalse(it!!) }
    }

    @Test
    fun `WHEN touch exploration state changes to true THEN voice view is enabled`() {
        repo.touchExplorationStateChangeListener.onTouchExplorationStateChanged(true)
        repo.isVoiceViewEnabled.observeForever { assertTrue(it!!) }
    }
}
