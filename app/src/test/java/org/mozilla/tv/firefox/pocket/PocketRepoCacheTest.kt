/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule

class PocketRepoCacheTest {

    @get:Rule
    val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var repoOutput: MutableLiveData<PocketVideoRepo.FeedState>
    private lateinit var cacheOutput: LiveData<PocketVideoRepo.FeedState>
    private lateinit var observerSpy: Observer<PocketVideoRepo.FeedState>
    private lateinit var repoCache: PocketRepoCache

    @Before
    fun setup() {
        repoOutput = MutableLiveData()
        val repo = object :
            PocketVideoRepo(mock(PocketEndpoint::class.java), PocketFeedStateMachine(), BuildConfigDerivables()) {
            override val feedState: LiveData<FeedState>
                get() = repoOutput
        }
        repoCache = PocketRepoCache(repo)
        cacheOutput = repoCache.feedState
        repoCache.unfreeze()
    }

    @Test
    fun `WHEN repo has not emitted anything THEN cache should not emit anything`() {
        observerSpy = spy(Observer { })
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(0)).onChanged(any())
    }

    @Test
    fun `WHEN repo emits non-load-complete values THEN cache should emit the same values`() {
        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.FetchFailed, it) })
        repoOutput.value = PocketVideoRepo.FeedState.FetchFailed
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)

        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.Loading, it) })
        repoOutput.value = PocketVideoRepo.FeedState.Loading
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)

        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.NoAPIKey, it) })
        repoOutput.value = PocketVideoRepo.FeedState.NoAPIKey
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)
    }

    @Test
    fun `GIVEN cache has received a value AND cache is not accepting updates WHEN future values are emitted from the repo THEN new values should not be emitted by the cache `() {
        val firstVideos = listOf(PocketViewModel.FeedItem.Video(1, "", "", "", 1))
        val secondVideos = listOf(PocketViewModel.FeedItem.Video(2, "", "", "", 2))
        assertNotEquals(firstVideos, secondVideos)

        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.LoadComplete(firstVideos), it) })
        repoOutput.value = PocketVideoRepo.FeedState.LoadComplete(firstVideos)
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)

        repoCache.freeze()

        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.LoadComplete(firstVideos), it) })
        repoOutput.value = PocketVideoRepo.FeedState.Loading
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)

        observerSpy = spy(Observer { assertEquals(PocketVideoRepo.FeedState.LoadComplete(firstVideos), it) })
        repoOutput.value = PocketVideoRepo.FeedState.LoadComplete(secondVideos)
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
        cacheOutput.removeObserver(observerSpy)
    }
}
