/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pinnedtile

import androidx.test.core.app.ApplicationProvider
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the [PinnedTileViewModel].
 */
@RunWith(RobolectricTestRunner::class)
class PinnedTileViewModelTest {

    private lateinit var pinnedTileViewModel: PinnedTileViewModel
    private lateinit var pinnedTileRepo: PinnedTileRepo
    private lateinit var testObserver: TestObserver<List<PinnedTile>>

    @Before
    fun setUp() {
        pinnedTileRepo = PinnedTileRepo(ApplicationProvider.getApplicationContext())
        pinnedTileViewModel = PinnedTileViewModel(pinnedTileRepo)
        testObserver = pinnedTileViewModel.tileList.test()
    }

    @Test
    fun `WHEN repo emits a successful load THEN view model should emit a list of same number of tiles`() {
        assertEquals(10, testObserver.values().last().size)
        assertEquals(1, testObserver.values().size)
    }

    @Test
    fun `WHEN repo emits an updated list after add THEN view model should emit an updated list`() {
        assertEquals(10, testObserver.values().last().size)
        pinnedTileRepo.addPinnedTile("https://example.com", null)
        assertEquals(11, testObserver.values().last().size)
        assertEquals(2, testObserver.values().size)
    }

    @Test
    fun `WHEN repo emits an updated list after remove THEN view model should emit an updated list`() {
        assertEquals(10, testObserver.values().last().size)
        pinnedTileViewModel.unpin("https://www.instagram.com/")
        assertEquals(9, testObserver.values().last().size)
        assertEquals(2, testObserver.values().size)
    }

    @Test
    fun `WHEN repo fails to remove an item THEN view model should emit nothing`() {
        assertEquals(10, testObserver.values().last().size)
        pinnedTileViewModel.unpin("https://example.com/")
        assertEquals(10, testObserver.values().last().size)
        assertEquals(1, testObserver.values().size)
    }
}
