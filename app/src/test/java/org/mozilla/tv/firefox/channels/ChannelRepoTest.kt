/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.channels.content.ChannelContent
import org.mozilla.tv.firefox.channels.content.getMusicChannels
import org.mozilla.tv.firefox.channels.content.getNewsChannels
import org.mozilla.tv.firefox.channels.content.getSportsChannels
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileImageUtilWrapper
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

@RunWith(FirefoxRobolectricTestRunner::class)
class ChannelRepoTest {
    private val fakeTiles = listOf(
        fakeChannelTile("www.mozilla.org"),
        fakeChannelTile("www.google.com"),
        fakeChannelTile("www.wikipedia.org"),
        fakeChannelTile("www.yahoo.com")
    )
    private val fakeTileObservable: Observable<List<ChannelTile>> = Observable.just(fakeTiles)

    @MockK private lateinit var pinnedTileRepo: PinnedTileRepo
    @MockK private lateinit var imageUtilWrapper: PinnedTileImageUtilWrapper
    @MockK private lateinit var formattedDomainWrapper: FormattedDomainWrapper
    private lateinit var channelRepo: ChannelRepo

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { pinnedTileRepo.pinnedTiles } answers { Observable.just(LinkedHashMap()) }

        channelRepo = ChannelRepo(ApplicationProvider.getApplicationContext(), imageUtilWrapper, formattedDomainWrapper, pinnedTileRepo)
    }

    @Test
    fun `WHEN blacklist is empty THEN filterNotBlacklisted should not change its input`() {
        val blacklist = Observable.just(setOf<String>())

        fakeTileObservable.filterNotBlacklisted(blacklist)
            .test()
            .assertValue(fakeTiles)
    }

    @Test
    fun `WHEN blacklist includes values in the list THEN filterNotBlacklisted should filter out these values`() {
        val blacklist = Observable.just(setOf("www.yahoo.com", "www.wikipedia.org"))

        fakeTileObservable.filterNotBlacklisted(blacklist)
            .map { tiles -> tiles.map { it.url } }
            .test()
            .assertValue(listOf("www.mozilla.org", "www.google.com"))
    }

    @Test
    fun `WHEN blacklist includes values not found in the original list THEN hte original list should be unexpected`() {
        val blacklist = Observable.just(setOf("www.bing.com"))

        fakeTileObservable.filterNotBlacklisted(blacklist).test()
            .assertValue(fakeTiles)
    }

    @Test // sanity check that we're no longer adding duplicated tiles to pad channels.
    fun `GIVEN an empty blacklist THEN the channel repo returns a number of tiles equal to the data source for each channel`() {
        fun assertDataSourceCountEqualsRepo(dataSource: List<ChannelTile>, channelRepoTiles: Observable<List<ChannelTile>>) {
            assertEquals(dataSource.size, channelRepoTiles.blockingFirst().size)
        }

        assertDataSourceCountEqualsRepo(ChannelContent.getNewsChannels(), channelRepo.getNewsTiles())
        assertDataSourceCountEqualsRepo(ChannelContent.getSportsChannels(), channelRepo.getSportsTiles())
        assertDataSourceCountEqualsRepo(ChannelContent.getMusicChannels(), channelRepo.getMusicTiles())
    }
}

private fun fakeChannelTile(url: String) = ChannelTile(
    url = url,
    title = url,
    subtitle = null,
    setImage = ImageSetStrategy.ById(0),
    tileSource = TileSource.BUNDLED,
    id = url
)
