/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.helpers.PocketTestData

class PocketVideoJSONValidatorTest {

    @Suppress("DEPRECATION")
    @MockK private lateinit var pocketVideoParser: PocketVideoParser

    private lateinit var validator: PocketVideoJSONValidator

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        validator = PocketVideoJSONValidator(pocketVideoParser)
    }

    @Test
    fun `WHEN validating for saving THEN the json argument is passed verbatim into the parser convert function`() {
        val json = "{ }"
        val slot = slot<String>()
        every { pocketVideoParser.convertVideosJSON(capture(slot)) } returns null
        validator.isJSONValidForSaving(json)

        assertEquals(json, slot.captured)
    }

    @Test
    fun `WHEN converting json to pocket videos THEN it delegates to the pocket parser`() {
        val json = "{ }"
        val slot = slot<String>()
        every { pocketVideoParser.convertVideosJSON(capture(slot)) } returns null
        validator.convertJSONToPocketVideos(json)

        assertEquals(json, slot.captured)
    }

    @Test
    fun `WHEN validating for saving and converted videos is null THEN return false`() {
        every { pocketVideoParser.convertVideosJSON(any()) } returns null
        assertFalse(validator.isJSONValidForSaving("{ }"))
    }

    @Test
    fun `WHEN validating for saving and converted videos size is equal to required video count THEN return true`() {
        every { pocketVideoParser.convertVideosJSON(any()) } returns
            PocketTestData.getVideoFeed(PocketVideoJSONValidator.REQUIRED_POCKET_VIDEO_COUNT)
        assertTrue(validator.isJSONValidForSaving("{ }"))
    }

    @Test
    fun `WHEN validating for saving and converted videos size is less than required video count THEN return false`() {
        every { pocketVideoParser.convertVideosJSON(any()) } returns
            PocketTestData.getVideoFeed(PocketVideoJSONValidator.REQUIRED_POCKET_VIDEO_COUNT - 1)
        assertFalse(validator.isJSONValidForSaving("{ }"))
    }
}
