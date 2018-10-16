package org.mozilla.focus.ext

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ViewExtTest {

    lateinit var a: ViewGroup
    lateinit var b: ViewGroup
    lateinit var c: ViewGroup
    lateinit var d: ViewGroup
    lateinit var e: ViewGroup
    lateinit var f: ViewGroup

    /**
     * View tree structure.
     *
     *     a
     *    / \
     *   b  c
     *  / \
     * d  e
     *   /
     *  f
     */
    @Before
    fun setup() {
        a = FrameLayout(RuntimeEnvironment.application)
        b = FrameLayout(RuntimeEnvironment.application)
        c = FrameLayout(RuntimeEnvironment.application)
        d = FrameLayout(RuntimeEnvironment.application)
        e = FrameLayout(RuntimeEnvironment.application)
        f = FrameLayout(RuntimeEnvironment.application)

        a.addView(b)
        a.addView(c)

        b.addView(d)
        b.addView(e)

        e.addView(f)
    }

    @Test
    fun `own visibility impacts isEffectivelyVisible`() {
        assertTrue(a.isEffectivelyVisible)
        a.visibility = View.INVISIBLE
        assertFalse(a.isEffectivelyVisible)
        a.visibility = View.GONE
        assertFalse(a.isEffectivelyVisible)
    }

    @Test
    fun `descendent visibility does not impact isEffectivelyVisible`() {
        assertTrue(a.isEffectivelyVisible)
        b.visibility = View.INVISIBLE
        assertTrue(a.isEffectivelyVisible)
        c.visibility = View.GONE
        assertTrue(a.isEffectivelyVisible)
        d.visibility = View.INVISIBLE
        assertTrue(a.isEffectivelyVisible)
        e.visibility = View.GONE
        assertTrue(a.isEffectivelyVisible)
    }

    @Test
    fun `sibling visibility does not impact isEffectivelyVisible`() {
        assertTrue(b.isEffectivelyVisible)
        c.visibility = View.INVISIBLE
        assertTrue(a.isEffectivelyVisible)
        c.visibility = View.GONE
        assertTrue(a.isEffectivelyVisible)
    }

    @Test
    fun `ancestor visibility does impact isEffectivelyVisible`() {
        assertTrue(f.isEffectivelyVisible)
        a.visibility = View.INVISIBLE
        assertFalse(f.isEffectivelyVisible)
        a.visibility = View.GONE
        assertFalse(f.isEffectivelyVisible)
    }
}
