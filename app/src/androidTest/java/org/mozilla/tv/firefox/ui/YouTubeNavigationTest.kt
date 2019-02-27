/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.ui.robots.browser
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test for YouTube navigation including:
 * - Going back with hardware back acts as expected from YouTube tile and Pocket videos
 * - D-pad navigation works as expected
 * To be written:
 * - Checking that clicking on the YouTube tile, opening the overlay, waiting for loading to end, then closing the overlay
 *   does not disrupt navigation
 */
class YouTubeNavigationTest {

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
    }

    @Test
    fun youtubeNavigationTest() {
        val youtubeUrl = "youtube.com/tv"
        /**
         * YouTube TV, Test page 1, back to YouTube, Test page 2, back to YouTube, back out of YouTube
         * Expected: Overlay
         */
        navigationOverlay {
        }.openYouTubeAndWaitForRedirects(activityTestRule) {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(pages[0].url) {
            remoteBackAndWaitForYouTube(activityTestRule)
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(pages[1].url) {
            remoteBackAndWaitForYouTube(activityTestRule)
        }.openOverlay {
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
        }.openYouTubeAndWaitForRedirects(activityTestRule) {
        }.openOverlay {
        }.enterUrlAndEnterToBrowser(pages[1].url) {
            remoteBackAndWaitForYouTube(activityTestRule)
        }.openOverlay {
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
            backOutOfYouTube()
        }
        navigationOverlay {
            assertURLBarDisplaysHint()
        }.openYouTubeAndWaitForRedirects(activityTestRule) {
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

            backOutOfYouTube()
        }.openOverlay {
            assertURLBarDisplaysHint()
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
