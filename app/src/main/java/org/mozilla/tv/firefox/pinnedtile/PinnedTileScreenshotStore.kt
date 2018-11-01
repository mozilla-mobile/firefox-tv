/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.AnyThread
import android.support.annotation.VisibleForTesting
import android.support.annotation.WorkerThread
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.mozilla.tv.firefox.ext.arePixelsAllTheSame
import org.mozilla.tv.firefox.pinnedtile.PinnedTileScreenshotStore.DIR
import org.mozilla.tv.firefox.pinnedtile.PinnedTileScreenshotStore.uuidToFileSystemMutex
import java.io.File
import java.util.UUID

/**
 * The format with which to compress on disk. Our goals for storage:
 * - Store full resolution, with minimal artifacting: we don't know how we'll display the images in
 * future redesigns so we want to preserve them as best as possible.
 * - Take up minimal space: we don't have how many tiles the user will store.
 *
 * WebP was chosen through testing: it produces the smallest file sizes with the least amount of
 * visual artifacting but is the most computationally expensive to save (2000ms WebP vs. 200ms JPEG),
 * which is irrelevant for our infrequent screenshots.
 *
 * For file size comparisons, at 1080p, quality 50 on IMDB (a dense page):
 * - WebP is ~65 KiB with almost no artifacts
 * - JPEG is ~130 KiB with noticeable artifacts up close
 * - PNG is ~1.6MiB with no artifacts (it is lossless)
 */
private val COMPRESSION_FORMAT = Bitmap.CompressFormat.WEBP

/**
 * The quality argument to [Bitmap.compress]: this value was picked through testing (see
 * [COMPRESSION_FORMAT] for goals). Here are comparisons at visual quality:
 * - 100: Possibly lossless
 * - 50: Visual artifacts essentially unnoticeable
 * - 25: Colors are muted and text starts to have noticeable compression
 *
 * Some approximate data points:
 * - IMDB quality 100: 426 KiB
 * - IMDB quality 50: 65 KiB
 * - IMDB quality 25: 43.6 KiB
 * - Google Video quality 50: 16 KiB
 * - Google Video quality 25: 13 KiB
 *
 * Since the data storage difference between 50 and 25 is negligible but there is a noticeable drop
 * in visual quality, I chose to use 50. Since these screenshots will be shrunk, we could probably
 * use lower quality but I'm concerned downscaling will make visual artifacts more significant and I
 * didn't feel it was worth the time to test.
 */
private const val COMPRESSION_QUALITY = 50

private val BITMAP_FACTORY_OPTIONS = BitmapFactory.Options().apply {
    // When full resolution in memory, 1080p Bitmap takes up ~7.9MiB, no matter how it's been
    // compressed on disk. To save memory and reduce CPU downscale overhead, we downsample.
    //
    // In 1080p, our max resolution, screenshots are 1920x1080px and the tiles are 280x200px
    // (not dp). We divide the screenshots by 4, 480x270px, which fits in the tiles: these
    // Bitmaps take up ~0.5MiB in memory.
    inSampleSize = 4
}

/**
 * Storage for webpage screenshots used for the home tiles.
 *
 * We use UUIDs as identifiers for screenshots, rather than URLs (a natural choice) because:
 * - URLs can exceed the maximum file name length; UUIDs can't
 * - URLs can contain illegal file name characters; UUID's can't
 * - URLs (as Strings) need to be validated (e.g. is it blank?)
 * - The [CustomTilesManager] stores a unique identifier so we rely on it to provide one,
 * pushing the complexity there.
 *
 * This class is thread-safe: see [uuidToFileSystemMutex] javadoc for details.
 */
object PinnedTileScreenshotStore {

    @VisibleForTesting const val DIR = "home_screenshots"

    /**
     * A map from UUID to Mutex: we have one lock for each screenshot we try to access (by uuid).
     *
     * This locking is important to ensure the file is completely written before its first read.
     *
     * Sometimes file writes can block for a long time (#610) so it's important we don't lock all
     * reads and writes on a single write.
     */
    private val uuidToFileSystemMutex = mutableMapOf<UUID, Mutex>()

    /** @param uuid a unique identifier for this screenshot. */
    @AnyThread
    fun saveAsync(context: Context, uuid: UUID, screenshot: Bitmap) = GlobalScope.launch {
        if (!isScreenshotAcceptableAsHomeTile(screenshot)) {
            // We won't save this image, meaning we'll return null when we try to read it.
            // At the time of writing, this will fall back to placeholders.
            return@launch
        }

        getMutex(uuid).withLock {
            ensureParentDirs(context)

            val screenshotFile = getFileForUUID(context, uuid)
            screenshotFile.createNewFile()
            screenshotFile.outputStream().use {
                screenshot.compress(COMPRESSION_FORMAT, COMPRESSION_QUALITY, it)
            }
        }
    }

    /** @param a unique identifier for this screenshot. */
    @AnyThread
    fun removeAsync(context: Context, uuid: UUID) = GlobalScope.launch {
        getMutex(uuid).withLock {
            getFileForUUID(context, uuid).delete()
        }
    }

    /**
     * A blocking function to read a bitmap from the store.
     *
     * @param uuid unique identifier for this screenshot.
     * @return The decoded [Bitmap], or null if the file DNE or the bitmap could not be decoded.
     */
    @WorkerThread // file access.
    suspend fun read(context: Context, uuid: UUID) = getMutex(uuid).withLock { // TODO: consider timeout: #610
        val file = getFileForUUID(context, uuid)
        if (!file.exists()) {
            null
        } else {
            file.inputStream().use {
                BitmapFactory.decodeStream(it, null, BITMAP_FACTORY_OPTIONS)
            }
        }
    }

    @VisibleForTesting internal fun getFileForUUID(context: Context, uuid: UUID) = File(context.filesDir, getPathForUUID(uuid))

    private fun getMutex(uuid: UUID) = synchronized(uuidToFileSystemMutex) {
        uuidToFileSystemMutex.getOrPut(uuid) { Mutex() }
    }
}

private fun ensureParentDirs(context: Context) { File(context.filesDir, DIR).mkdirs() }
private fun getPathForUUID(uuid: UUID) = "$DIR/$uuid"

private fun isScreenshotAcceptableAsHomeTile(screenshot: Bitmap): Boolean {
    // Some websites get blank screenshots: vimeo videos are all black and Youtube videos
    // are all transparent. We don't want these.
    //
    // It'd be more accurate to add some delta when comparing pixels, but that adds
    // complexity that we we don't need for any screenshots we've already seen - Bitmap screenshots
    // don't have compression rounding errors - and may accidentally remove valid images.
    return !screenshot.arePixelsAllTheSame()
}
