/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.framework

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.helpers.ext.assertValues
import java.lang.IllegalStateException
import kotlin.properties.Delegates

class FrameworkRepoTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule() // necessary for LiveData tests.

    private lateinit var repo: FrameworkRepo

    // Different variants for different tests.
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var touchExplorationA11yManagerWrapper: MockTouchExplorationA11yManagerWrapper

    @Before
    fun setUp() {
        repo = FrameworkRepo()

        accessibilityManager = mock(AccessibilityManager::class.java)
        touchExplorationA11yManagerWrapper = MockTouchExplorationA11yManagerWrapper()
    }

    @Test
    fun `GIVEN the framework has voice view disabled WHEN init is called THEN the voice view is disabled`() {
        accessibilityManager.also {
            `when`(it.isTouchExplorationEnabled).thenReturn(false)
        }

        repo.isVoiceViewEnabled.assertValues(false) {
            repo.init(accessibilityManager)
        }
    }

    @Test
    fun `GIVEN the framework has voice view enabled WHEN init is called THEN the voice view is enabled`() {
        accessibilityManager.also {
            `when`(it.isTouchExplorationEnabled).thenReturn(true)
        }

        repo.isVoiceViewEnabled.assertValues(true) {
            repo.init(accessibilityManager)
        }
    }

    @Test
    fun `GIVEN init is called WHEN the framework updates touch exploration values THEN voice view enabled state is updated`() {
        repo.init(touchExplorationA11yManagerWrapper.mock)

        // To ensure the emission change logic is working, we make sure to test
        // in both directions irrespective of the initial value.
        val defaultValue = touchExplorationA11yManagerWrapper.isTouchExplorationStateEnabled
        repo.isVoiceViewEnabled.assertValues(defaultValue, false, true, false) {
            with(touchExplorationA11yManagerWrapper) {
                isTouchExplorationStateEnabled = false
                isTouchExplorationStateEnabled = true
                isTouchExplorationStateEnabled = false
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN init is called twice THEN an exception is thrown`() {
        repo.init(accessibilityManager)
        repo.init(accessibilityManager)
    }
}

/**
 * A wrapper to interact with an underlying mock [AccessibilityManager] instance that supports
 * [AccessibilityManager.addTouchExplorationStateChangeListener].
 *
 * It is necessary to write our own implementation because the Robolectric shadows do not support
 * [TouchExplorationStateChangeListener]s.
 *
 * Note that this does not recreate [AccessibilityManager.isEnabled] functionality:
 * [AccessibilityManager.isTouchExplorationEnabled] and friends are derived from that value so the application
 * never checks it and we don't need to model it either.
 */
private class MockTouchExplorationA11yManagerWrapper {

    val mock: AccessibilityManager = mock(AccessibilityManager::class.java).also {
        `when`(it.addTouchExplorationStateChangeListener(any())).then { invocationOnMock ->
            val listener = invocationOnMock.getArgument<TouchExplorationStateChangeListener>(0)
            touchExplorationStateChangeListeners.add(listener)
        }
    }

    private val touchExplorationStateChangeListeners = mutableListOf<TouchExplorationStateChangeListener>()

    var isTouchExplorationStateEnabled by Delegates.observable(mock.isTouchExplorationEnabled) { _, _, isTouchExplorationStateEnabled ->
        touchExplorationStateChangeListeners.forEach {
            it.onTouchExplorationStateChanged(isTouchExplorationStateEnabled)
        }
    }
}
