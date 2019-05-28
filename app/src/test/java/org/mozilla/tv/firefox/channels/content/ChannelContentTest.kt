package org.mozilla.tv.firefox.channels.content

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelContentTest {

    @Test
    fun `all tv guide channel tiles SHOULD have unique ids`() {
        val allTiles = ChannelContent.getMusicChannels() +
            ChannelContent.getNewsChannels() +
            ChannelContent.getSportsChannels()

        val tileCount = allTiles.size
        val uniqueIdCount = allTiles.map { it.id }
            .toSet()
            .size

        assertEquals(tileCount, uniqueIdCount)
    }
}
