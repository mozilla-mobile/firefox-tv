/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class PocketRepoCacheTest {

    private lateinit var repoOutput: Subject<PocketVideoRepo.FeedState>
    private lateinit var cacheOutput: Observable<PocketVideoRepo.FeedState>
    private lateinit var repoCache: PocketRepoCache
    private lateinit var testObserver: TestObserver<PocketVideoRepo.FeedState>

    @Before
    fun setup() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        repoOutput = PublishSubject.create()

        val repo = object :
            PocketVideoRepo(
                mock(PocketEndpoint::class.java),
                PocketFeedStateMachine(),
                PocketVideoRepo.FeedState.Loading
            ) {
            override val feedState: Observable<FeedState>
                get() = repoOutput
        }
        repoCache = PocketRepoCache(repo)
        cacheOutput = repoCache.feedState
        testObserver = repoCache.feedState.test()
    }

    @Test
    fun `WHEN repo has not emitted anything THEN cache should not emit anything`() {
        repoCache.frozen = false

        assertEquals(0, testObserver.valueCount())
    }

    @Test
    fun `GIVEN cache is unfrozen WHEN repo emits non-load-complete values THEN cache should emit the same values`() {
        repoCache.frozen = false

        repoOutput.onNext(PocketVideoRepo.FeedState.FetchFailed)
        repoOutput.onNext(PocketVideoRepo.FeedState.Loading)
        repoOutput.onNext(PocketVideoRepo.FeedState.NoAPIKey)

        testObserver.assertValues(PocketVideoRepo.FeedState.FetchFailed,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey)
    }

    @Test
    fun `GIVEN cache has received a valid value AND cache is frozen WHEN future values are emitted from the repo THEN new values should not be emitted by the cache `() {
        val firstVideos = listOf(PocketViewModel.FeedItem.Video(1, "", "", "", 1, ""))
        val secondVideos = listOf(PocketViewModel.FeedItem.Video(2, "", "", "", 2, ""))
        assertNotEquals(firstVideos, secondVideos)

        repoOutput.onNext(PocketVideoRepo.FeedState.LoadComplete(firstVideos))
        repoCache.frozen = true
        repoOutput.onNext(PocketVideoRepo.FeedState.Loading)
        repoOutput.onNext(PocketVideoRepo.FeedState.LoadComplete(secondVideos))

        testObserver.assertValue(PocketVideoRepo.FeedState.LoadComplete(firstVideos))
    }

    @Test
    fun `GIVEN cache has not received a valid value AND cache is frozen WHEN future values are emitted from the repo THEN new values should be emitted by the cache`() {
        repoCache.frozen = true

        repoOutput.onNext(PocketVideoRepo.FeedState.FetchFailed)
        repoOutput.onNext(PocketVideoRepo.FeedState.Loading)
        repoOutput.onNext(PocketVideoRepo.FeedState.NoAPIKey)

        testObserver.assertValues(
            PocketVideoRepo.FeedState.FetchFailed,
            PocketVideoRepo.FeedState.Loading,
            PocketVideoRepo.FeedState.NoAPIKey
        )
    }
}
