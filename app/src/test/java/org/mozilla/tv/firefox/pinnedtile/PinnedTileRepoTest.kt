/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.util.UUID

private const val BUNDLED_TILE_COUNT = 10

/**
 * Unit tests for the [PinnedTileRepo].
 * Bundled tile list fetch comes from /assets/bundled/bundled_tiles.json
 */
@RunWith(RobolectricTestRunner::class)
class PinnedTileRepoTest {

    private lateinit var pinnedTileRepo: PinnedTileRepo
    private val importantBundled = linkedMapOf(
            "https://ftv.cdn.mozilla.net/ytht" to BundledPinnedTile(url = "https://ftv.cdn.mozilla.net/ytht", title = "YouTube", imagePath = "tile_youtube.png", id = "youtube"),
            "https://www.google.com/webhp?tbm=vid" to BundledPinnedTile(url = "https://www.google.com/webhp?tbm=vid", title = "Video Search", imagePath = "tile_google.png", id = "googleVideo")
    )
    private val unimportantBundled = linkedMapOf(
            "https://m.imdb.com/" to BundledPinnedTile(url = "https://m.imdb.com/", title = "IMDB", imagePath = "tile_imdb.png", id = "imdb"),
            "https://www.rottentomatoes.com/" to BundledPinnedTile(url = "https://www.rottentomatoes.com/", title = "Rotten Tomatoes", imagePath = "tile_rotten_tomatoes.png", id = "rottenTomatoes")
    )
    private val bundledTiles = linkedMapOf<String, BundledPinnedTile>().apply {
        putAll(importantBundled)
        putAll(unimportantBundled)
    }
    private val customTiles = linkedMapOf(
            "" to CustomPinnedTile(url = "https://www.mozilla.com", title = "Mozilla", id = UUID.randomUUID()),
            "" to CustomPinnedTile(url = "https://www.amazon.com", title = "Amazon", id = UUID.randomUUID())
    )

    @Before
    fun setUp() {
        pinnedTileRepo = PinnedTileRepo(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `WHEN repo fetches initial tileList THEN repo should emit a feed of only bundled tile list`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assertEquals(BUNDLED_TILE_COUNT, (it as LinkedHashMap<String, PinnedTile>).size)
        })

        @Suppress("DEPRECATION")
        pinnedTileRepo.legacyPinnedTiles.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo adds a new pin THEN repo should emit a combined list of bundled and custom`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT || it.size == BUNDLED_TILE_COUNT + 1)
        })

        @Suppress("DEPRECATION")
        pinnedTileRepo.legacyPinnedTiles.observeForever(observerSpy)
        assertEquals(0, pinnedTileRepo.customTilesSize)
        pinnedTileRepo.addPinnedTile("https://example.com", null)
        assertEquals(1, pinnedTileRepo.customTilesSize)

        verify(observerSpy, times(2)).onChanged(any())
    }

    @Test
    fun `WHEN repo removes a bundled pin THEN repo should emit a smaller list and bundled size`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT || it.size == BUNDLED_TILE_COUNT - 1)
        })

        @Suppress("DEPRECATION")
        pinnedTileRepo.legacyPinnedTiles.observeForever(observerSpy)
        assertEquals(10, pinnedTileRepo.bundledTilesSize)
        pinnedTileRepo.removePinnedTile("https://www.pinterest.com/")
        assertEquals(9, pinnedTileRepo.bundledTilesSize)

        verify(observerSpy, times(2)).onChanged(any())
    }

    @Test
    fun `WHEN repo fails to remove a bundled pin THEN repo should maintain same bundled size`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT)
        })

        @Suppress("DEPRECATION")
        pinnedTileRepo.legacyPinnedTiles.observeForever(observerSpy)
        assertEquals(10, pinnedTileRepo.bundledTilesSize)
        pinnedTileRepo.removePinnedTile("https://example.com/")
        assertEquals(10, pinnedTileRepo.bundledTilesSize)

        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo removes a custom pin THEN repo should emit a smaller list and custom size`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT || it.size == BUNDLED_TILE_COUNT + 1)
        })

        @Suppress("DEPRECATION")
        pinnedTileRepo.legacyPinnedTiles.observeForever(observerSpy)
        assertEquals(0, pinnedTileRepo.customTilesSize)
        pinnedTileRepo.addPinnedTile("https://example.com", null)
        assertEquals(1, pinnedTileRepo.customTilesSize)
        pinnedTileRepo.removePinnedTile("https://example.com")
        assertEquals(0, pinnedTileRepo.customTilesSize)

        verify(observerSpy, times(3)).onChanged(any())
    }

    @Test
    fun `GIVEN no bundled tiles have been removed AND no custom tiles have been added THEN loadTilesCache should match base bundled tiles `() {
        val actual = pinnedTileRepo.loadTilesCache(bundledTiles, linkedMapOf())
        assertEquals(bundledTiles, actual)
    }

    @Test
    fun `GIVEN no bundled tiles have been removed AND custom tiles have been added THEN loadTilesCache should show youtube and google then custom then other bundled`() {
        val actual = pinnedTileRepo.loadTilesCache(bundledTiles, customTiles)
        val expected = linkedMapOf<String, PinnedTile>().apply {
            putAll(importantBundled)
            putAll(customTiles)
            putAll(unimportantBundled)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN all important tiles have been removed AND custom tiles have been added THEN loadTilesCache should show custom tiles then unimportant bundled`() {
        val actual = pinnedTileRepo.loadTilesCache(unimportantBundled, customTiles)
        val expected = linkedMapOf<String, PinnedTile>().apply {
            putAll(customTiles)
            putAll(unimportantBundled)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN one important value remains AND one bundled value remains AND custom tiles have been pinned THEN loadTilesCache should show the important value then custom tiles then unimpotant bundled tiles`() {
        val firstImportant = importantBundled.entries.first()
        val lastUnimportant = importantBundled.entries.last()
        val modifiedBundledTiles = linkedMapOf<String, BundledPinnedTile>().apply {
            put(firstImportant.key, firstImportant.value)
            put(lastUnimportant.key, lastUnimportant.value)
        }
        val actual = pinnedTileRepo.loadTilesCache(modifiedBundledTiles, customTiles)
        val expected = linkedMapOf<String, PinnedTile>().apply {
            put(firstImportant.key, firstImportant.value)
            putAll(customTiles)
            put(lastUnimportant.key, lastUnimportant.value)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN the default bundled tiles WHEN a tile is added THEN it is added after the important tiles`() {
        val addedTileURL = "https://what-a-crazy-added-pinned-tile-eh.com"
        pinnedTileRepo.addPinnedTile(addedTileURL, screenshot = null)
        val actualTiles = pinnedTileRepo.pinnedTiles.test().values().last()
        val expectedAddedIndex = importantBundled.size // index of first custom tile
        val actualAddedIndex = actualTiles.keys.indexOf(addedTileURL)
        assertEquals(expectedAddedIndex, actualAddedIndex)
    }
}
