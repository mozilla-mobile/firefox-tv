/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import android.arch.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the [PinnedTileViewModel].
 */
@RunWith(RobolectricTestRunner::class)
class PinnedTileViewModelTest {

    private lateinit var pinnedTileViewModel: PinnedTileViewModel
    private lateinit var pinnedTileRepo: PinnedTileRepo

    @Before
    fun setUp() {
        pinnedTileRepo = PinnedTileRepo(ApplicationProvider.getApplicationContext())
        pinnedTileViewModel = PinnedTileViewModel(pinnedTileRepo)
    }

    @Test
    fun `WHEN repo emits a successful load THEN view model should emit a list of same number of tiles`() {
        val observerSpy = Mockito.spy(Observer<List<PinnedTile>> {
            Assert.assertTrue(it is List<PinnedTile>)
            Assert.assertEquals(11, it!!.size)
        })

        pinnedTileViewModel.getTileList().observeForever(observerSpy)

        Mockito.verify(observerSpy, Mockito.times(1)).onChanged(ArgumentMatchers.any())
    }

    @Test
    fun `WHEN repo emits an updated list after add THEN view model should emit an updated list`() {
        val observerSpy = Mockito.spy(Observer<List<PinnedTile>> {
            Assert.assertTrue(it is List<PinnedTile>)
            assert(it!!.size == 11 || it.size == 12)
        })

        pinnedTileViewModel.getTileList().observeForever(observerSpy)
        pinnedTileRepo.addPinnedTile("https://example.com", null)

        Mockito.verify(observerSpy, Mockito.times(2)).onChanged(ArgumentMatchers.any())
    }

    @Test
    fun `WHEN repo emits an updated list after remove THEN view model should emit an updated list`() {
        val observerSpy = Mockito.spy(Observer<List<PinnedTile>> {
            Assert.assertTrue(it is List<PinnedTile>)
            assert(it!!.size == 11 || it.size == 10)
        })

        pinnedTileViewModel.getTileList().observeForever(observerSpy)
        pinnedTileViewModel.unpin("https://www.instagram.com/")

        Mockito.verify(observerSpy, Mockito.times(2)).onChanged(ArgumentMatchers.any())
    }

    @Test
    fun `WHEN repo fails to remove an item THEN view model should emit nothing`() {
        val observerSpy = Mockito.spy(Observer<List<PinnedTile>> {
            Assert.assertTrue(it is List<PinnedTile>)
            // it.size == 10 is the original load emission
            assert(it!!.size == 11)
        })

        pinnedTileViewModel.getTileList().observeForever(observerSpy)
        pinnedTileViewModel.unpin("https://example.com/")

        Mockito.verify(observerSpy, Mockito.times(1)).onChanged(ArgumentMatchers.any())
    }
}
