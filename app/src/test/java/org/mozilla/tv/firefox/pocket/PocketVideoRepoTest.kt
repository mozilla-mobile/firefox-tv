/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.PocketTestData
import org.mozilla.tv.firefox.helpers.RxTestHelper
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class PocketVideoRepoTest {

    companion object {
        @BeforeClass @JvmStatic
        fun beforeClass() {
            RxTestHelper.forceRxSynchronousInBeforeClass()
        }
    }

    private lateinit var repo: PocketVideoRepo
    @MockK private lateinit var videoStore: PocketVideoStore
    private var isPocketEnabledByLocale = false

    private lateinit var feedState: BehaviorSubject<FeedState>
    private lateinit var feedStateTestObs: TestObserver<FeedState>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        feedState = BehaviorSubject.create()
        feedStateTestObs = feedState.test()
        repo = PocketVideoRepo(videoStore, { isPocketEnabledByLocale }, feedState)
    }

    @Test
    fun `GIVEN feedState is no api key WHEN updating Pocket from store THEN feedState is still no api key`() {
        feedState.onNext(FeedState.NoAPIKey)

        repo.updatePocketFromStore()

        feedStateTestObs.assertValues(FeedState.NoAPIKey)
    }

    @Test
    fun `GIVEN pocket is not enabled by locale WHEN updating Pocket from store THEN feedState becomes inactive`() {
        repo.updatePocketFromStore()

        feedStateTestObs.assertValues(FeedState.Inactive)
    }

    @Test
    fun `GIVEN pocket is enabled by locale WHEN updating Pocket from store THEN feedState has videos loaded by store`() {
        isPocketEnabledByLocale = true
        val videos = PocketTestData.getVideoFeed(3)
        every { videoStore.load() } returns videos
        repo.updatePocketFromStore()

        feedStateTestObs.assertValues(FeedState.LoadComplete(videos))
    }

    @Test
    fun `GIVEN an existing feed state WHEN updating Pocket from store THEN feedState can change between the expected values`() {
        val videos = PocketTestData.getVideoFeed(3)
        every { videoStore.load() } returns videos
        feedState.onNext(FeedState.Inactive)

        isPocketEnabledByLocale = true
        repo.updatePocketFromStore()

        isPocketEnabledByLocale = false
        repo.updatePocketFromStore()

        isPocketEnabledByLocale = true
        repo.updatePocketFromStore()

        feedStateTestObs.assertValues(
            FeedState.Inactive,
            FeedState.LoadComplete(videos),
            FeedState.Inactive,
            FeedState.LoadComplete(videos)
        )
    }

    @Test // sanity check
    fun `WHEN getting a new instance THEN no exception is thrown`() {
        PocketVideoRepo.newInstance(videoStore, { true }, true)
    }

    @Test
    fun `GIVEN pocket key is valid WHEN getting a new instance THEN feed state starts inactive`() {
        // This test uses different instances than the ones created in the @Before block
        val repo = PocketVideoRepo.newInstance(videoStore, { true }, isPocketKeyValid = true)
        repo.feedState.test().assertValue(FeedState.Inactive)
    }

    @Test
    fun `GIVEN pocket key is not valid WHEN getting a new instance THEN feed state starts no api key`() {
        // This test uses different instances than the ones created in the @Before block
        val repo = PocketVideoRepo.newInstance(videoStore, { true }, isPocketKeyValid = false)
        repo.feedState.test().assertValue(FeedState.NoAPIKey)
    }
}
