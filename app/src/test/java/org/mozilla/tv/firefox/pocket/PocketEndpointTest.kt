/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mozilla.tv.firefox.TestResource
import org.robolectric.RobolectricTestRunner

private const val KEY_INNER = "recommendations"

@RunWith(RobolectricTestRunner::class)
class PocketEndpointTest {

    private lateinit var pocketEndpoint: PocketEndpoint

    @Before
    fun setup() {
        pocketEndpoint = PocketEndpoint(mock(PocketEndpointRaw::class.java)) { true }
    }

    @Test
    fun `convert Videos JSON successfully to internal objects`() {
        val expectedSubset = listOf(
            PocketViewModel.FeedItem.Video(
                id = 27587,
                title = "How a Master Pastry Chef Uses Architecture to Make Sky High Pastries",
                url = "https://www.youtube.com/tv#/watch/video/idle?v=953Qt4FnAcU",
                thumbnailURL = "https://img-getpocket.cdn.mozilla.net/direct?url=http%3A%2F%2Fimg.youtube.com%2Fvi%2F953Qt4FnAcU%2Fmaxresdefault.jpg&resize=w450",
                popularitySortId = 20,
                authors = "Eater"
            ),
            PocketViewModel.FeedItem.Video(
                id = 27581,
                title = "How Does Having Too Much Power Affect Your Brain?",
                url = "https://www.youtube.com/watch?v=GHZ7-kq3GDQ",
                thumbnailURL = "https://img-getpocket.cdn.mozilla.net/direct?url=http%3A%2F%2Fimg.youtube.com%2Fvi%2FGHZ7-kq3GDQ%2Fmaxresdefault.jpg&resize=w450",
                popularitySortId = 17,
                // TODO: Update this to be the platform, once the behaviour is fixed (issue #1484)
                authors = ""
            )
        )

        val pocketJSON = TestResource.POCKET_VIDEO_RECOMMENDATION.get()
        val actualVideos = pocketEndpoint.convertVideosJSON(pocketJSON)
        if (actualVideos == null) { fail("Expected actualVideos to be non-null"); return }

        // We only test a subset of the data for developer sanity. :)
        assertEquals(20, actualVideos.size)
        expectedSubset.forEachIndexed { i, expected ->
            assertEquals(expected, actualVideos[i])
        }
    }

    @Test
    fun `convert Videos JSON for videos with missing fields drops those items`() {
        val pocketJSON = TestResource.POCKET_VIDEO_RECOMMENDATION.get()

        val expectedFirstTitle = JSONObject(pocketJSON).getJSONArray(KEY_INNER).getJSONObject(0).getString("title")
        assertNotNull(expectedFirstTitle)

        val pocketJSONWithNoTitleExceptFirst = removeTitleStartingAtIndex(1, pocketJSON)
        val actualVideos = pocketEndpoint.convertVideosJSON(pocketJSONWithNoTitleExceptFirst)
        if (actualVideos == null) { fail("Expected videos non-null"); return }
        assertEquals(1, actualVideos.size)
        assertEquals(expectedFirstTitle, actualVideos[0].title)
    }

    @Test
    fun `convert Videos JSON for videos with missing fields on all items`() {
        val pocketJSON = TestResource.POCKET_VIDEO_RECOMMENDATION.get()
        val pocketJSONWithNoTitles = removeTitleStartingAtIndex(0, pocketJSON)
        val actualVideos = pocketEndpoint.convertVideosJSON(pocketJSONWithNoTitles)
        assertNull(actualVideos)
    }

    @Test
    fun `convert Videos JSON for empty String`() {
        assertNull(pocketEndpoint.convertVideosJSON(""))
    }

    @Test
    fun `convert Videos JSON for invalid JSON`() {
        assertNull(pocketEndpoint.convertVideosJSON("{!!}}"))
    }
}

private fun removeTitleStartingAtIndex(startIndex: Int, json: String): String {
    val obj = JSONObject(json)
    val videosJson = obj.getJSONArray(KEY_INNER)
    for (i in startIndex until videosJson.length()) {
        videosJson.getJSONObject(i).remove("title")
    }
    return obj.toString()
}
