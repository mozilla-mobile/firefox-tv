/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.AnyThread
import android.support.annotation.VisibleForTesting
import android.support.annotation.WorkerThread
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.home.HomeTileScreenshotStore.DIR
import java.io.File
import java.util.UUID
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

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
 * This class is thread-safe: see lock javadoc for details.
 */
object HomeTileScreenshotStore {

    @VisibleForTesting const val DIR = "home_screenshots"

    /**
     * A lock on all file system operations: we acquire an exclusive lock for any writes and a
     * shared lock for any reads. In practice, we should have little contention: reads happen only
     * when showing the home tiles while writes can only happen when we pin or unpin a site, neither
     * of which should happen concurrently with a read. This locking is important to ensure the
     * initial write completes before any reads.
     *
     * We could do more granular locking by having a lock per file/URL/screenshot, but that would be
     * more complex for little gain, given the little contention we expect.
     *
     * One downside to using a [ReentrantReadWriteLock] is that it's not coroutine aware: in the
     * event of contention, threads from the common thread pool will be removed from scheduling
     * until the write lock returns, forcing the thread pool to spin up more threads if there are
     * more readers. However, coroutine aware solutions (e.g. [Mutex]) would require more
     * coarse-grained locking that will generate contention, or more complexity, which isn't worth
     * it given our low chances of contention.
     */
    private val fileSystemLock = ReentrantReadWriteLock()

    /** @param uuid a unique identifier for this screenshot. */
    @AnyThread
    fun saveAsync(context: Context, uuid: UUID, screenshot: Bitmap) = launch {
        fileSystemLock.write {
            ensureParentDirs(context)

            val screenshotFile = getFileForUUID(context, uuid)
            screenshotFile.createNewFile()
            screenshotFile.outputStream().use {
                screenshot.compress(COMPRESSION_FORMAT, COMPRESSION_QUALITY, it)
            }
        }
    }

    /**
     * A blocking function to read a bitmap from the store.
     *
     * TODO: Add downscale argument.
     * When full size in memory, a 1080p Bitmap takes up ~7.9MiB, no matter how it's been compressed
     * on disk.
     *
     * @param uuid unique identifier for this screenshot.
     * @return The decoded [Bitmap], or null if the file DNE or the bitmap could not be decoded.
     */
    @WorkerThread // file access.
    fun read(context: Context, uuid: UUID): Bitmap? {
        return fileSystemLock.read {
            val file = getFileForUUID(context, uuid)
            if (!file.exists()) {
                null
            } else {
                file.inputStream().use {
                    BitmapFactory.decodeStream(it)
                }
            }
        }
    }

    @VisibleForTesting internal fun getFileForUUID(context: Context, uuid: UUID) = File(context.filesDir, getPathForUUID(uuid))
}

private fun ensureParentDirs(context: Context) { File(context.filesDir, DIR).mkdirs() }
private fun getPathForUUID(uuid: UUID) = "$DIR/$uuid"
