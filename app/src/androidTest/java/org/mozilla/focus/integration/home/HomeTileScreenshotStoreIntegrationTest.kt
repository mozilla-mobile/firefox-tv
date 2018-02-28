/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.integration.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.assertEqualsWithDelta
import org.mozilla.focus.home.HomeTileScreenshotStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HomeTileScreenshotStoreIntegrationTest {

    private lateinit var andyContext: Context // coroutines shadow `context` name.

    @Before
    fun setUp() {
        andyContext = InstrumentationRegistry.getTargetContext()
        andyContext.filesDir.listFiles().forEach { it.deleteRecursively() } // Clean up between tests.
    }

    /**
     * Tests that the bitmap we save to the store is the same one we get out.
     * This must be run on device because Robolectric replaces Bitmap with
     * ShadowBitmap, which overrides the compress method. We could try to
     * mock around it or write a custom shadow, but it's probably not worth it.
     */
    @Test
    fun testSaveAndReadRestoresBitmap() = runBlocking {
        // We use a bitmap that won't change much after compression.
        val expectedBitmap = getRedSquare()
        val uuid = UUID.randomUUID()
        HomeTileScreenshotStore.saveAsync(andyContext, uuid, expectedBitmap).join()

        val actualBitmap = HomeTileScreenshotStore.read(andyContext, uuid)

        // Delta chosen by testing against our compression quality.
        assertNotNull(actualBitmap)
        assertEqualsWithDelta(expectedBitmap, actualBitmap!!, 7f)
    }
}

private fun getRedSquare(): Bitmap {
    val dimen = 140
    val colors = IntArray(dimen * dimen) { Color.RED }
    return Bitmap.createBitmap(colors, dimen, dimen, Bitmap.Config.ARGB_8888)
}
