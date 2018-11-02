package org.mozilla.tv.firefox.ext

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ContextTest {

    private var context: Context? = null
    private var accessibilityManager: AccessibilityManager? = null

    @Before
    fun startUp() {
        context = ApplicationProvider.getApplicationContext()
        accessibilityManager = context!!.getAccessibilityManager()
    }

    @Test
    fun `isVoiceViewEnabled() returns true when TouchExploration enabled`() {
        shadowOf(accessibilityManager).setTouchExplorationEnabled(true)
        assertTrue(context!!.isVoiceViewEnabled())
    }

    @Test
    fun `isVoiceViewEnabled() returns false when TouchExploration disabled`() {
        shadowOf(accessibilityManager).setTouchExplorationEnabled(false)
        assertFalse(context!!.isVoiceViewEnabled())
    }

    @Test
    fun `getAccessibilityManager() provides a working instance`() {
        shadowOf(accessibilityManager).setEnabled(true)
        assertTrue(accessibilityManager!!.isEnabled)
        assertTrue(getAccessibilityManagerInstance().isEnabled)
        shadowOf(accessibilityManager).setEnabled(false)
        assertFalse(accessibilityManager!!.isEnabled)
        assertFalse(getAccessibilityManagerInstance().isEnabled)
    }

    @Throws(Exception::class)
    private fun getAccessibilityManagerInstance(): AccessibilityManager {
        return ReflectionHelpers.callStaticMethod(AccessibilityManager::class.java, "getInstance",
                ReflectionHelpers.ClassParameter.from(Context::class.java, context))
    }
}
