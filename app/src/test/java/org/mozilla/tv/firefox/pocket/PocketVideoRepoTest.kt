package org.mozilla.tv.firefox.pocket

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.ext.assertValues
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PocketVideoRepoTest {

    @get:Rule val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var pocketVideoRepo: PocketVideoRepo

    @Before
    fun setup() {
        initWithLanguage { true }
    }

    fun initWithLanguage(isEnglish: () -> Boolean) {
        val pocketEndpoint = object : PocketEndpoint("1", "www.test.com".toUri()) {
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
        }

        pocketVideoRepo = PocketVideoRepo(
            pocketEndpoint = pocketEndpoint,
            localeIsEnglish = isEnglish,
            pocketFeedStateMachine = PocketFeedStateMachine(),
            buildConfigDerivables = BuildConfigDerivables(isEnglish)
        )
    }

    @Test
    fun `WHEN language is not english THEN repo should emit inactive state`() {
        initWithLanguage(isEnglish = { false })
        pocketVideoRepo.feedState.assertValues(PocketVideoRepo.FeedState.Inactive) {}
    }
}
