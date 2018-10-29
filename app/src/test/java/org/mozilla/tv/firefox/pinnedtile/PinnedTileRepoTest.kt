/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.arch.lifecycle.Observer
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

private const val BUNDLED_TILE_COUNT = 10

/**
 * Unit tests for the [PinnedTileRepo].
 * Bundled tile list fetch comes from /assets/bundled/bundled_tiles.json
 */
@RunWith(RobolectricTestRunner::class)
class PinnedTileRepoTest {

    private lateinit var pinnedTileRepo: PinnedTileRepo

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

        pinnedTileRepo.getPinnedTiles().observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo adds a new pin THEN repo should emit a combined list of bundled and custom`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT || it.size == BUNDLED_TILE_COUNT + 1)
        })

        pinnedTileRepo.getPinnedTiles().observeForever(observerSpy)
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

        pinnedTileRepo.getPinnedTiles().observeForever(observerSpy)
        assertEquals(10, pinnedTileRepo.bundledTilesSize)
        pinnedTileRepo.removePinnedTile("https://pinterest.com/")
        assertEquals(9, pinnedTileRepo.bundledTilesSize)

        verify(observerSpy, times(2)).onChanged(any())
    }

    @Test
    fun `WHEN repo fails to remove a bundled pin THEN repo should maintain same bundled size`() {
        val observerSpy = spy(Observer<LinkedHashMap<String, PinnedTile>> {
            assertNotNull(it)
            assert((it as LinkedHashMap<String, PinnedTile>).size == BUNDLED_TILE_COUNT)
        })

        pinnedTileRepo.getPinnedTiles().observeForever(observerSpy)
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

        pinnedTileRepo.getPinnedTiles().observeForever(observerSpy)
        assertEquals(0, pinnedTileRepo.customTilesSize)
        pinnedTileRepo.addPinnedTile("https://example.com", null)
        assertEquals(1, pinnedTileRepo.customTilesSize)
        pinnedTileRepo.removePinnedTile("https://example.com")
        assertEquals(0, pinnedTileRepo.customTilesSize)

        verify(observerSpy, times(3)).onChanged(any())
    }
}
