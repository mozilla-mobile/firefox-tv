/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule

class PocketViewModelTest {

    @Rule
    @JvmField
    var rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var viewModel: PocketViewModel
    private lateinit var repoCacheState: MutableLiveData<PocketVideoRepo.FeedState>
    private lateinit var observerSpy: Observer<PocketViewModel.State>

    private lateinit var loadingPlaceholders: List<PocketViewModel.FeedItem>
    private lateinit var noKeyPlaceholders: List<PocketViewModel.FeedItem>

    @Before
    fun setup() {

        repoCacheState = MutableLiveData()
        val repo = mock(PocketVideoRepo::class.java)
        `when`(repo.feedState).thenReturn(MutableLiveData())
        val repoCache = object : PocketRepoCache(repo) {
            override val feedState: MutableLiveData<PocketVideoRepo.FeedState>
                get() = repoCacheState
        }

        viewModel = PocketViewModel(repo, repoCache)
        loadingPlaceholders = viewModel.loadingPlaceholders
        noKeyPlaceholders = viewModel.noKeyPlaceholders
    }

    @Test
    fun `WHEN repo cache emits a successful fetch THEN view model should emit a feed with the same videos`() {
        val videos = listOf<PocketViewModel.FeedItem>(
            PocketViewModel.FeedItem.Video(1, "", "", "", 1, "")
        )

        observerSpy = spy(Observer {
            assertTrue(it is PocketViewModel.State.Feed)
            assertEquals(videos, (it as PocketViewModel.State.Feed).feed)
        })

        viewModel.state.observeForever(observerSpy)

        repoCacheState.value = PocketVideoRepo.FeedState.LoadComplete(videos)

        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo cache emits loading THEN view model should emit a feed of placeholders`() {
        observerSpy = spy(Observer {
            assertTrue(it is PocketViewModel.State.Feed)
            assertEquals(loadingPlaceholders, (it as PocketViewModel.State.Feed).feed)
        })

        viewModel.state.observeForever(observerSpy)

        repoCacheState.value = PocketVideoRepo.FeedState.Loading

        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo cache emits no key THEN view model should emit a feed of no key placeholders`() {
        observerSpy = spy(Observer {
            assertTrue(it is PocketViewModel.State.Feed)
            assertEquals(noKeyPlaceholders, (it as PocketViewModel.State.Feed).feed)
        })

        viewModel.state.observeForever(observerSpy)

        repoCacheState.value = PocketVideoRepo.FeedState.NoAPIKey

        verify(observerSpy, times(1)).onChanged(any())
    }

    @Test
    fun `WHEN repo cache emits failure THEN view model should emit an error state`() {
        observerSpy = spy(Observer {
            assertTrue(it === PocketViewModel.State.Error)
        })

        viewModel.state.observeForever(observerSpy)

        repoCacheState.value = PocketVideoRepo.FeedState.FetchFailed

        verify(observerSpy, times(1)).onChanged(any())
    }
}
