/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID

/**
 * Unit tests for the screenshot store. Though HomeTileScreenshotStoreIntegrationTest exists,
 * we run these as unit tests to conserve resources.
 *
 * We could improve the tests by additionally testing:
 * - Read/write locking works correctly.
 */
@RunWith(RobolectricTestRunner::class)
class PinnedTileScreenshotStoreUnitTest {

    private lateinit var uuid: UUID

    @Before
    fun setUp() {
        RuntimeEnvironment.application.filesDir.listFiles().forEach { it.deleteRecursively() }
        uuid = UUID.randomUUID()
    }

    /** Assumes [PinnedTileScreenshotStore.getFileForUUID] works correctly. */
    @Test
    fun testSaveAsyncDoesNotOverwrite() = runBlocking {
        val context = RuntimeEnvironment.application
        PinnedTileScreenshotStore.saveAsync(context, uuid, getNonBlankBitmap()).join()
        PinnedTileScreenshotStore.saveAsync(context, UUID.randomUUID(), getNonBlankBitmap()).join()

        assertEquals(2,
                PinnedTileScreenshotStore.getFileForUUID(context, uuid).parentFile.list().size)
    }

    @Test
    fun testSaveAsyncDoesNotSaveBlankScreenshot() = runBlocking {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val context = RuntimeEnvironment.application
        PinnedTileScreenshotStore.saveAsync(context, uuid, bitmap).join()

        val parentDir = PinnedTileScreenshotStore.getFileForUUID(context, uuid).parentFile
        parentDir.mkdirs() // so we can easily assert without crashing if dirs weren't made.
        assertEquals(0, parentDir.list().size)
    }

    @Test
    fun testReadFileDoesNotExist() = runBlocking {
        val actualBitmap = PinnedTileScreenshotStore.read(RuntimeEnvironment.application, uuid)
        assertNull(actualBitmap)
    }

    /** Assumes [PinnedTileScreenshotStore.getFileForUUID] works correctly. */
    @Test
    fun testRemoveAsyncRemovesFile() = runBlocking {
        val context = RuntimeEnvironment.application
        val file = PinnedTileScreenshotStore.getFileForUUID(context, uuid)
        file.parentFile.mkdirs()
        file.createNewFile()
        file.writeText("Some test text")
        assertEquals(1, file.parentFile.list().size)

        PinnedTileScreenshotStore.removeAsync(context, uuid).join()

        assertEquals(0, file.parentFile.list().size)
    }

    /**
     * Assumes [PinnedTileScreenshotStore.saveAsync] and [PinnedTileScreenshotStore.getFileForUUID]
     * works correctly.
     */
    @Test
    fun testRemoveAsyncRemovesFileWrittenBySaveAsync() = runBlocking {
        val context = RuntimeEnvironment.application
        val parentFile = PinnedTileScreenshotStore.getFileForUUID(context, uuid).parentFile
        PinnedTileScreenshotStore.saveAsync(context, uuid, getNonBlankBitmap()).join()
        assertEquals(1, parentFile.list().size)

        PinnedTileScreenshotStore.removeAsync(context, uuid).join()

        assertEquals(0, parentFile.list().size)
    }
}

/** Gets a bitmap that isn't a single color: we don't save blank bitmaps, so this is important. */
private fun getNonBlankBitmap() = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
    eraseColor(Color.RED)
    setPixel(0, 0, Color.rgb(244, 0, 0))
}
