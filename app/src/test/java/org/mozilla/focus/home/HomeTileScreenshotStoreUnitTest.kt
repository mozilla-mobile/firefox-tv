/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.graphics.Bitmap
import android.graphics.Color
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import kotlinx.coroutines.experimental.runBlocking
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
class HomeTileScreenshotStoreUnitTest {

    private lateinit var uuid: UUID

    @Before
    fun setUp() {
        RuntimeEnvironment.application.filesDir.listFiles().forEach { it.deleteRecursively() }
        uuid = UUID.randomUUID()
    }

    /** Assumes [HomeTileScreenshotStore.getFileForUUID] works correctly. */
    @Test
    fun testSaveAsyncDoesNotOverwrite() = runBlocking {
        val context = RuntimeEnvironment.application
        HomeTileScreenshotStore.saveAsync(context, uuid, getNonBlankBitmap()).join()
        HomeTileScreenshotStore.saveAsync(context, UUID.randomUUID(), getNonBlankBitmap()).join()

        assertEquals(2,
                HomeTileScreenshotStore.getFileForUUID(context, uuid).parentFile.list().size)
    }

    @Test
    fun testSaveAsyncDoesNotSaveBlankScreenshot() = runBlocking {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val context = RuntimeEnvironment.application
        HomeTileScreenshotStore.saveAsync(context, uuid, bitmap).join()

        val parentDir = HomeTileScreenshotStore.getFileForUUID(context, uuid).parentFile
        parentDir.mkdirs() // so we can easily assert without crashing if dirs weren't made.
        assertEquals(0, parentDir.list().size)
    }

    @Test
    fun testReadFileDoesNotExist() {
        val actualBitmap = HomeTileScreenshotStore.read(RuntimeEnvironment.application, uuid)
        assertNull(actualBitmap)
    }

    /** Assumes [HomeTileScreenshotStore.getFileForUUID] works correctly. */
    @Test
    fun testRemoveAsyncRemovesFile() = runBlocking {
        val context = RuntimeEnvironment.application
        val file = HomeTileScreenshotStore.getFileForUUID(context, uuid)
        file.parentFile.mkdirs()
        file.createNewFile()
        file.writeText("Some test text")
        assertEquals(1, file.parentFile.list().size)

        HomeTileScreenshotStore.removeAsync(context, uuid).join()

        assertEquals(0, file.parentFile.list().size)
    }

    /**
     * Assumes [HomeTileScreenshotStore.saveAsync] and [HomeTileScreenshotStore.getFileForUUID]
     * works correctly.
     */
    @Test
    fun testRemoveAsyncRemovesFileWrittenBySaveAsync() = runBlocking {
        val context = RuntimeEnvironment.application
        val parentFile = HomeTileScreenshotStore.getFileForUUID(context, uuid).parentFile
        HomeTileScreenshotStore.saveAsync(context, uuid, getNonBlankBitmap()).join()
        assertEquals(1, parentFile.list().size)

        HomeTileScreenshotStore.removeAsync(context, uuid).join()

        assertEquals(0, parentFile.list().size)
    }
}

/** Gets a bitmap that isn't a single color: we don't save blank bitmaps, so this is important. */
private fun getNonBlankBitmap() = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
    eraseColor(Color.RED)
    setPixel(0, 0, Color.rgb(244, 0, 0))
}
