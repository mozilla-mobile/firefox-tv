/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapTest {

    @Test
    fun testAreAllPixelsTheSameAllRed() {
        val testBitmap = getNewBitmap()
        testBitmap.eraseColor(Color.RED)

        assertTrue(testBitmap.arePixelsAllTheSame())
    }

    @Test
    fun testAreAllPixelsTheSameAllTransparent() {
        val testBitmap = getNewBitmap()
        testBitmap.eraseColor(Color.TRANSPARENT)

        assertTrue(testBitmap.arePixelsAllTheSame())
    }

    @Test
    fun testAreAllPixelsTheSameOneOff() {
        val testBitmap = getNewBitmap()
        testBitmap.eraseColor(Color.RED)

        testBitmap.setPixel(0, 1, Color.rgb(244, 0, 0))

        assertFalse(testBitmap.arePixelsAllTheSame())
    }
}

private fun getNewBitmap() = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
