/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.helpers.ext.assertValues
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
            PocketVideoRepo(
                mock(PocketEndpoint::class.java),
                PocketFeedStateMachine(),
                { true },
                mock(BuildConfigDerivables::class.java)
            ) {
            override val feedState: LiveData<FeedState>
                get() = repoOutput
        }
        repoCache = PocketRepoCache(repo)
        cacheOutput = repoCache.feedState
        repoCache.setup()
    }

    @Test
    fun `WHEN repo has not emitted anything THEN cache should not emit anything`() {
        repoCache.frozen = false

        observerSpy = spy(Observer { })
        cacheOutput.observeForever(observerSpy)
        verify(observerSpy, times(0)).onChanged(any())
    }

    @Test
    fun `GIVEN cache is unfrozen WHEN repo emits non-load-complete values THEN cache should emit the same values`() {
        repoCache.frozen = false

        repoCache.feedState.assertValues(
            PocketVideoRepo.FeedState.FetchFailed,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey
        ) {
            repoOutput.value = PocketVideoRepo.FeedState.FetchFailed
            repoOutput.value = PocketVideoRepo.FeedState.Loading
            repoOutput.value = PocketVideoRepo.FeedState.NoAPIKey
        }
    }

    @Test
    fun `GIVEN cache has received a valid value AND cache is frozen WHEN future values are emitted from the repo THEN new values should not be emitted by the cache `() {
        val firstVideos = listOf(PocketViewModel.FeedItem.Video(1, "", "", "", 1, ""))
        val secondVideos = listOf(PocketViewModel.FeedItem.Video(2, "", "", "", 2, ""))
        assertNotEquals(firstVideos, secondVideos)

        repoCache.frozen = false

        repoCache.feedState.assertValues(PocketVideoRepo.FeedState.LoadComplete(firstVideos)) {
            repoOutput.value = PocketVideoRepo.FeedState.LoadComplete(firstVideos)
            repoCache.frozen = true
            repoOutput.value = PocketVideoRepo.FeedState.Loading
            repoOutput.value = PocketVideoRepo.FeedState.LoadComplete(secondVideos)
        }
    }

    @Test
    fun `GIVEN cache has not received a valid value AND cache is frozen WHEN future values are emitted from the repo THEN new values should be emitted by the cache`() {
        repoCache.frozen = true

        repoCache.feedState.assertValues(
            PocketVideoRepo.FeedState.FetchFailed,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey
        ) {
            repoOutput.value = PocketVideoRepo.FeedState.FetchFailed
            repoOutput.value = PocketVideoRepo.FeedState.Loading
            repoOutput.value = PocketVideoRepo.FeedState.NoAPIKey
        }
    }
}
