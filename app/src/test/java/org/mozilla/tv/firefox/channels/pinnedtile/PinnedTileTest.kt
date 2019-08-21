/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.pinnedtile

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.navigationoverlay.NavigationOverlayViewModel
import org.mozilla.tv.firefox.channels.ChannelDetails
import org.mozilla.tv.firefox.channels.ChannelRepo
import org.mozilla.tv.firefox.navigationoverlay.ChannelTitles
import org.mozilla.tv.firefox.navigationoverlay.ToolbarViewModel
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner
import java.io.File

const val DEFAULT_PINNED_TILE_COUNT = 10

/**
 * Unit tests for pinned tile operations in [NavigationOverlayViewModel].
 */
@Ignore(value = "This test needs to be rethought. Most of the tested functionality currently " +
    "belongs to the ChannelRepo, not the PinnedTileRepo.  Should these classes be merged?")
@RunWith(FirefoxRobolectricTestRunner::class)
class PinnedTileTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        }
    }

    @MockK private lateinit var sessionRepo: SessionRepo
    @MockK private lateinit var screenController: ScreenController
    @MockK private lateinit var channelRepo: ChannelRepo
    @MockK private lateinit var pinnedTileImageUtilWrapper: PinnedTileImageUtilWrapper
    @MockK private lateinit var formattedDomainWrapper: FormattedDomainWrapper
    @MockK private lateinit var drawable: Drawable
    @MockK private lateinit var file: File
    private lateinit var overlayVm: NavigationOverlayViewModel
    private lateinit var pinnedTileRepo: PinnedTileRepo
    private lateinit var testObserver: TestObserver<ChannelDetails>
    private lateinit var channelTitles: ChannelTitles

    @Suppress("DEPRECATION")
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { pinnedTileImageUtilWrapper.generatePinnedTilePlaceholder(any()) } answers { drawable }
        every { pinnedTileImageUtilWrapper.getFileForUUID(any()) } answers { file }
        every { formattedDomainWrapper.format(any(), any(), any()) } answers { "" }
        every { channelRepo.getNewsTiles() } answers { Observable.just(listOf()) }
        every { channelRepo.getMusicTiles() } answers { Observable.just(listOf()) }
        every { channelRepo.getSportsTiles() } answers { Observable.just(listOf()) }

        channelTitles = ChannelTitles(
            pinned = "pinned",
            newsAndPolitics = "news",
            sports = "sports",
            music = "music",
            food = "food"
        )

        val appContext: Context = ApplicationProvider.getApplicationContext()
        pinnedTileRepo = PinnedTileRepo(appContext)
        overlayVm = NavigationOverlayViewModel(
                screenController,
                channelTitles,
                channelRepo,
                ToolbarViewModel(
                    sessionRepo = sessionRepo,
                    pinnedTileRepo = pinnedTileRepo
                ),
                mockk(),
                mockk()
        )
        testObserver = overlayVm.pinnedTiles.test()
    }

    @Test
    fun `WHEN repo emits a successful load THEN view model should emit a list of same number of tiles`() {
        assertEquals(1, testObserver.valueCount())
        assertEquals(DEFAULT_PINNED_TILE_COUNT, testObserver.values().last().tileList.size)
    }

    @Test
    fun `WHEN repo emits an updated list after add THEN view model should emit an updated list`() {
        assertEquals(DEFAULT_PINNED_TILE_COUNT, testObserver.values().last().tileList.size)
        pinnedTileRepo.addPinnedTile("https://example.com", null)
        assertEquals(2, testObserver.valueCount())
        assertEquals(DEFAULT_PINNED_TILE_COUNT + 1, testObserver.values().last().tileList.size)
    }

    @Test
    fun `WHEN repo emits an updated list after remove THEN view model should emit an updated list`() {
        assertEquals(DEFAULT_PINNED_TILE_COUNT, testObserver.values().last().tileList.size)
//        overlayVm.unpinPinnedTile("https://www.instagram.com/")
        assertEquals(2, testObserver.valueCount())
        assertEquals(DEFAULT_PINNED_TILE_COUNT - 1, testObserver.values().last().tileList.size)
    }

    @Test
    fun `WHEN repo fails to remove an item THEN view model should emit nothing`() {
        assertEquals(DEFAULT_PINNED_TILE_COUNT, testObserver.values().last().tileList.size)
//        overlayVm.unpinPinnedTile("https://example.com/")
        assertEquals(1, testObserver.valueCount())
        assertEquals(DEFAULT_PINNED_TILE_COUNT, testObserver.values().last().tileList.size)
    }
}
