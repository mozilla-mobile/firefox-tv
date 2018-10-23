/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.integration.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.assertEqualsWithDelta
import org.mozilla.tv.firefox.pinnedtile.HomeTileScreenshotStore
import java.util.UUID

private const val DIMEN = 140 // should be divisible by 4.

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
        val originalBitmap = getRedishSquare()

        val uuid = UUID.randomUUID()
        HomeTileScreenshotStore.saveAsync(andyContext, uuid, originalBitmap).join()

        // Reading scales the bitmap by 4 so they don't take up as much space in memory.
        val expectedDimen = DIMEN / 4
        val expectedBitmap = Bitmap.createScaledBitmap(originalBitmap, expectedDimen, expectedDimen, false)
        val actualBitmap = HomeTileScreenshotStore.read(andyContext, uuid)

        // Delta chosen by testing against our compression quality.
        assertNotNull(actualBitmap)
        assertEqualsWithDelta(expectedBitmap, actualBitmap!!, 7f)
    }
}

/* Return almost solid color square: we don't save completely blank tiles so we change a pixel. */
private fun getRedishSquare() = Bitmap.createBitmap(DIMEN, DIMEN, Bitmap.Config.ARGB_8888).apply {
    eraseColor(Color.RED)
    setPixel(0, 0, Color.rgb(244, 0, 0))
}
