/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import android.app.Application
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.TestDependencyFactory
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.CustomPocketFeedStateProvider
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.ui.robots.browser
import org.mozilla.tv.firefox.ui.robots.navigationOverlay
import org.mozilla.tv.firefox.utils.ServiceLocator

/**
 * A test for YouTube navigation including:
 * - Going back with hardware back acts as expected from YouTube tile and Pocket videos
 * - D-pad navigation works as expected
 * - Checking that clicking on the YouTube tile, opening the overlay, waiting for loading to end, then closing the overlay
 *   does not disrupt navigation
 */
class YouTubeNavigationTest {
    companion object : TestDependencyFactory {
        private val customPocketFeedStateProvider = CustomPocketFeedStateProvider()

        override fun createServiceLocator(app: Application) = object : ServiceLocator(app) {
            override val pocketRepo = customPocketFeedStateProvider.fakedPocketRepo
        }
    }

    @get:Rule val activityTestRule = MainActivityTestRule()
    private lateinit var server: MockWebServer
    private lateinit var pages: List<TestAssetHelper.TestAsset>

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    @Before
    fun setup() {
        server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }

        pages = TestAssetHelper.getGenericAssets(server)

        val mockedState = PocketVideoRepo.FeedState.LoadComplete(listOf(
            PocketViewModel.FeedItem.Video(
                id = 27587,
                title = "How a Master Pastry Chef Uses Architecture to Make Sky High Pastries",
                url = "https://www.youtube.com/tv#/watch/video/idle?v=953Qt4FnAcU",
                thumbnailURL = "https://img-getpocket.cdn.mozilla.net/direct?url=http%3A%2F%2Fimg.youtube.com%2Fvi%2F953Qt4FnAcU%2Fmaxresdefault.jpg&resize=w450",
                popularitySortId = 20,
                authors = "Eater"
            )
        ))

        customPocketFeedStateProvider.fakedPocketRepoState.onNext(mockedState)
    }

    @Test
    fun youtubeNavigationTest() {
        val youtubeUrl = "youtube.com/tv"
        /**
         * YouTube TV, Test page 1, back to YouTube, Test page 2, back to YouTube, back out of YouTube
         * Expected: Overlay
         */
        navigationOverlay {
        }.enterUrlAndEnterToBrowser(youtubeUrl.toUri()!!) {
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
        }.enterUrlAndEnterToBrowser(pages[0].url) {
            remoteBack()
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
        }.enterUrlAndEnterToBrowser(pages[1].url) {
            remoteBack()
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
            assertURLBarTextContains(youtubeUrl)
        }.closeToBrowser {
            backOutOfYouTube()
        }.openOverlay {
            assertURLBarDisplaysHint()

        /**
         * Test page 3, YouTube TV, Test page 2, back to YouTube, back out of Youtube
         * Expected: Test page 3
         */
        }.enterUrlAndEnterToBrowser(pages[2].url) {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(youtubeUrl.toUri()!!) {
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
        }.enterUrlAndEnterToBrowser(pages[1].url) {
            remoteBack()
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
            assertURLBarTextContains(youtubeUrl)
        }.closeToBrowser {
            backOutOfYouTube()
        }.openOverlay {
            assertURLBarTextContains(pages[2].url.toString())
        }

        /**
         * First, go back to the beginning of history.
         * YouTube from Pocket, back out of YouTube, YouTube TV, back out of YouTube
         * Expected: Overlay
         */
        .closeToBrowser {
            remoteBack()
        }
        navigationOverlay {
        }.openPocketMegatile {
        }.openTileToBrowser(0) {
        }.openOverlay {
            waitUntilYouTubeVideoLoads()
        }.closeToBrowser {
            backOutOfYouTube()
        }
        navigationOverlay {
            waitForURLBarToDisplayHint()
            assertURLBarDisplaysHint()
        }.enterUrlAndEnterToBrowser(youtubeUrl.toUri()!!) {
        }.openOverlay {
            waitUntilYouTubeHomeLoads()
        }.closeToBrowser {
            backOutOfYouTube()
        }.openOverlay {
            assertURLBarDisplaysHint()

        /**
         * Open YouTube, open overlay before loading is complete, wait for loading to complete,
         * make sure YouTube navigation still works (regression test for #1830).
         */
            disableSessionIdling(activityTestRule)
        }
        .enterUrlAndEnterToBrowser(youtubeUrl.toUri()!!) {
        }.openOverlay {
            enableSessionIdling(activityTestRule)
        }.closeToBrowser {
            /**
             * Test dpad navigation.
             * The only unique attribute between video elements is the 'aria-label'.
             * Compare this label before and after dpad navigation to ensure focus changes.
             */
            val ariaLabelAttribute = "getAttribute('aria-label')"
            val firstElementLabel = getActiveElementAttribute(ariaLabelAttribute)
            dpadRight()
            assert(firstElementLabel != getActiveElementAttribute(ariaLabelAttribute))
            dpadLeft()
            assert(firstElementLabel == getActiveElementAttribute(ariaLabelAttribute))
        }
    }

    /**
     * Clicks remote back until the active element is in the YouTube sidebar.
     * Then, clicks remote back one more time to go to the previous site.
     */
    private fun backOutOfYouTube() {
        browser {
            var activeElementParentId = getActiveElementAttribute("parentElement.parentElement.id")
            while (activeElementParentId != "guide-list") {
                remoteBack()
                activeElementParentId = getActiveElementAttribute("parentElement.parentElement.id")
            }
            remoteBack()
        }
    }
}
