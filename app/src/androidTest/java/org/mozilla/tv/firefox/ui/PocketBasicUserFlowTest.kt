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
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.CustomPocketFeedStateProvider
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.ui.robots.DeviceButton
import org.mozilla.tv.firefox.ui.robots.navigationOverlay
import org.mozilla.tv.firefox.utils.ServiceLocator

/**
 * Tests the Pocket happy path.
 * - Open Pocket recommended videos from home
 * - Back from Pocket recommended videos screen to home
 * - Reopen Pocket recommended videos
 * - Click on a video
 */
class PocketBasicUserFlowTest {

    companion object : TestDependencyFactory {
        private val customPocketFeedStateProvider = CustomPocketFeedStateProvider()

        override fun createServiceLocator(app: Application) = object : ServiceLocator(app) {
            override val pocketRepo = customPocketFeedStateProvider.fakedPocketRepo
        }
    }

    @get:Rule val activityTestRule = MainActivityTestRule()

    private lateinit var page: TestAssetHelper.TestAsset

    @Before
    fun setup() {

        val server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }

        page = TestAssetHelper.getGenericAssets(server).first()

        val mockedState = PocketVideoRepo.FeedState.LoadComplete(listOf(
            PocketViewModel.FeedItem.Video(
                id = 0,
                title = "Title",
                url = page.url.toString(),
                thumbnailURL = "https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png",
                popularitySortId = 0,
                authors = "youtube, bbc"
            )
        ))

        customPocketFeedStateProvider.fakedPocketRepoState.postValue(mockedState)
    }

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun pocketBasicUserFlowTest() {
        navigationOverlay {
        }.openPocketMegatile {
        }.returnToOverlay(DeviceButton.BACK) {
            // This is to test that pressing back from Pocket screen goes to overlay
        }.openPocketMegatile {
        }.returnToOverlay(DeviceButton.MENU) {
            // This is to test that pressing menu from Pocket screen goes to overlay
        }.openPocketMegatile {
        }.openTileToBrowser(0) {
            assertTestContent(page.content)
        }
    }
}
