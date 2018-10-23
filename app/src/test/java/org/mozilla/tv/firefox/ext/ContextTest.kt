package org.mozilla.tv.firefox.ext

import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ContextTest {

    private var context: Context? = null
    private var accessibilityManager: AccessibilityManager? = null

    @Before
    fun startUp() {
        context = RuntimeEnvironment.application
        accessibilityManager = context!!.getAccessibilityManager()
    }

    @Test
    fun `isVoiceViewEnabled() returns true when TouchExploration enabled`() {
        shadowOf(accessibilityManager).isTouchExplorationEnabled = true
        assertTrue(context!!.isVoiceViewEnabled())
    }

    @Test
    fun `isVoiceViewEnabled() returns false when TouchExploration disabled`() {
        shadowOf(accessibilityManager).isTouchExplorationEnabled = false
        assertFalse(context!!.isVoiceViewEnabled())
    }

    @Test
    fun `getAccessibilityManager() provides a working instance`() {
        shadowOf(accessibilityManager).isEnabled = true
        assertTrue(accessibilityManager!!.isEnabled)
        assertTrue(getAccessibilityManagerInstance().isEnabled)
        shadowOf(accessibilityManager).isEnabled = false
        assertFalse(accessibilityManager!!.isEnabled)
        assertFalse(getAccessibilityManagerInstance().isEnabled)
    }

    @Throws(Exception::class)
    private fun getAccessibilityManagerInstance(): AccessibilityManager {
        return ReflectionHelpers.callStaticMethod(AccessibilityManager::class.java, "getInstance",
                ReflectionHelpers.ClassParameter.from(Context::class.java, RuntimeEnvironment.application))
    }
}
