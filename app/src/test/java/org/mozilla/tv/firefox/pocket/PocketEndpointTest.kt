/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.TestResource
import org.robolectric.RobolectricTestRunner

private const val KEY_INNER = "recommendations"

@RunWith(RobolectricTestRunner::class)
class PocketEndpointTest {

    @Test
    fun `convert Videos JSON successfully to internal objects`() {
        val expectedSubset = listOf(
            PocketFeedItem.Video(
                id = 23227,
                title = "How Hands Became the Internet’s New Selfie | Internetting Season 2",
                url = "https://www.youtube.com/tv#/watch/video/idle?v=IzvXi8VkxgI",
                thumbnailURL = "https://img-getpocket.cdn.mozilla.net/direct?url=http%3A%2F%2Fimg.youtube.com%2Fvi%2FIzvXi8VkxgI%2Fmaxresdefault.jpg&resize=w450",
                popularitySortId = 1
            ),
            PocketFeedItem.Video(
                id = 23222,
                title = "Why The US Airforce Ejected a Bear",
                url = "https://www.youtube.com/watch?v=Rbt2NFCtBh4",
                thumbnailURL = "https://img-getpocket.cdn.mozilla.net/direct?url=http%3A%2F%2Fimg.youtube.com%2Fvi%2FRbt2NFCtBh4%2Fmaxresdefault.jpg&resize=w450",
                popularitySortId = 2
            )
        )

        val pocketJSON = TestResource.POCKET_VIDEO_RECOMMENDATION.get()
        val actualVideos = PocketEndpoint.convertVideosJSON(pocketJSON)
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
        val actualVideos = PocketEndpoint.convertVideosJSON(pocketJSONWithNoTitleExceptFirst)
        if (actualVideos == null) { fail("Expected videos non-null"); return }
        assertEquals(1, actualVideos.size)
        assertEquals(expectedFirstTitle, actualVideos[0].title)
    }

    @Test
    fun `convert Videos JSON for videos with missing fields on all items`() {
        val pocketJSON = TestResource.POCKET_VIDEO_RECOMMENDATION.get()
        val pocketJSONWithNoTitles = removeTitleStartingAtIndex(0, pocketJSON)
        val actualVideos = PocketEndpoint.convertVideosJSON(pocketJSONWithNoTitles)
        assertNull(actualVideos)
    }

    @Test
    fun `convert Videos JSON for empty String`() {
        assertNull(PocketEndpoint.convertVideosJSON(""))
    }

    @Test
    fun `convert Videos JSON for invalid JSON`() {
        assertNull(PocketEndpoint.convertVideosJSON("{!!}}"))
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
