/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class PocketViewModelTest {

    private lateinit var viewModel: PocketViewModel
    private lateinit var repoCacheState: Subject<PocketVideoRepo.FeedState>
    private lateinit var testObserver: TestObserver<PocketViewModel.State>

    private lateinit var noKeyPlaceholders: List<PocketViewModel.FeedItem>

    @Before
    fun setup() {
        repoCacheState = PublishSubject.create()
        val repo = mock(PocketVideoRepo::class.java)
        `when`(repo.feedState).thenReturn(repoCacheState)

        viewModel = PocketViewModel(repo)
        noKeyPlaceholders = PocketViewModel.noKeyPlaceholders
        testObserver = viewModel.state.test()
    }

    @Test
    fun `WHEN repo cache emits a successful fetch THEN view model should emit a feed with the same videos`() {
        val videos = listOf<PocketViewModel.FeedItem>(
            PocketViewModel.FeedItem.Video(1, "", "", "", 1, "")
        )

        repoCacheState.onNext(PocketVideoRepo.FeedState.LoadComplete(videos))

        assertEquals(1, testObserver.valueCount())

        testObserver.values()[0].let {
            assertTrue(it is PocketViewModel.State.Feed)
            assertEquals(videos, (it as PocketViewModel.State.Feed).feed)
        }
    }

    @Test
    fun `WHEN repo cache emits no key THEN view model should emit a feed of no key placeholders`() {
        repoCacheState.onNext(PocketVideoRepo.FeedState.NoAPIKey)

        assertEquals(1, testObserver.valueCount())

        testObserver.values()[0].let {
            assertTrue(it is PocketViewModel.State.Feed)
            assertEquals(noKeyPlaceholders, (it as PocketViewModel.State.Feed).feed)
        }
    }

    @Test
    fun `WHEN repo cache emits inactive THEN view model should emit not displayed`() {
        repoCacheState.onNext(PocketVideoRepo.FeedState.Inactive)

        assertEquals(1, testObserver.valueCount())

        assertEquals(PocketViewModel.State.NotDisplayed, testObserver.values()[0])
    }
}
