package org.mozilla.tv.firefox.pocket

import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ext.toUri
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.helpers.ext.assertValues
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PocketVideoRepoTest {

    @get:Rule val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var pocketVideoRepoSpy: PocketVideoRepo
    private lateinit var pocketEndpointSpy: PocketEndpoint

    private fun initWithLanguage(isEnglish: () -> Boolean) {
        pocketEndpointSpy = spy(object : PocketEndpoint("1", "www.test.com".toUri()) {
            override suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? =
                listOf(
                    PocketViewModel.FeedItem.Video(
                        1,
                        "Some video",
                        "some url",
                        "thumbnail",
                        1,
                        "author"
                    )
                )
        })

        pocketVideoRepoSpy = spy(PocketVideoRepo(
            pocketEndpoint = pocketEndpointSpy,
            localeIsEnglish = isEnglish,
            pocketFeedStateMachine = PocketFeedStateMachine(),
            buildConfigDerivables = BuildConfigDerivables(isEnglish, "MOCK-POCKET-KEY")
        ))
    }

    @Test
    fun `WHEN language is not english THEN repo should emit inactive state`() {
        initWithLanguage(isEnglish = { false })
        pocketVideoRepoSpy.feedState.assertValues(PocketVideoRepo.FeedState.Inactive) {
            pocketVideoRepoSpy.update()
        }
    }

    // Verifying that suspend functions have been called only works if the test and prod
    // code share the same coroutine scope. To fix this test, we will need to find a
    // workaround for this.
    // See https://github.com/nhaarman/mockito-kotlin/issues/247
    @Ignore
    @Test
    fun `WHEN language is not english THEN pocket endpoint should never be called`() {
        initWithLanguage(isEnglish = { false })
        pocketVideoRepoSpy.feedState.observeForever { /* start subscription */ }
        runBlocking {
            pocketVideoRepoSpy.update()
            verify(pocketVideoRepoSpy, times(1)).updateInner()
            verify(pocketEndpointSpy, times(0)).getRecommendedVideos()
        }
    }
}
