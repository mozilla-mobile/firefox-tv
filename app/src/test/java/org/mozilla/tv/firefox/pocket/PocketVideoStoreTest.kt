/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.test.core.app.ApplicationProvider
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.helpers.PocketTestData
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

private val POCKET_FEED_TEST_DATA = PocketTestData.getVideoFeed(1)

@RunWith(FirefoxRobolectricTestRunner::class)
class PocketVideoStoreTest {
    private lateinit var pocketVideoStore: PocketVideoStore
    private lateinit var sharedPrefs: SharedPreferences

    @MockK private lateinit var context: Context
    @MockK private lateinit var assetManager: AssetManager
    @MockK private lateinit var jsonValidator: PocketVideoJSONValidator

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        sharedPrefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("PocketFetch", 0)
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs

        pocketVideoStore = PocketVideoStore(context, assetManager, jsonValidator)
    }

    @Test
    fun `WHEN the raw json is valid THEN it is saved in shared preferences`() {
        every { jsonValidator.isJSONValidForSaving(any()) } returns true
        val expected = "{ }"

        assertTrue(pocketVideoStore.save(expected))

        val actual = sharedPrefs.getString("video_json", null)

        assertEquals(expected, actual)
    }

    @Test
    fun `WHEN the raw json is invalid THEN it is not saved in shared preferences`() {
        every { jsonValidator.isJSONValidForSaving(any()) } returns false

        assertFalse(pocketVideoStore.save("{ }"))
    }

    @Test
    fun `WHEN saving THEN the json to save is passed verbatim into the json validator`() {
        arrayOf("{ }", "", " ", "{ ").forEachIndexed { i, json ->
            // The return value doesn't matter.
            val validatorJSONCaptured = slot<String>()
            every { jsonValidator.isJSONValidForSaving(capture(validatorJSONCaptured)) } returns true

            pocketVideoStore.save(json)

            println("Input index $i: $json")
            assertEquals(json, validatorJSONCaptured.captured)
        }
    }

    @Test
    fun `GIVEN shared preferences is empty WHEN loading THEN the bundled tiles json is passed verbatim into the json validator`() {
        val expectedBundledJSON = "assets"
        everyAssetManagerOpensInputStream(expectedBundledJSON.toInputStream())

        // The return value doesn't matter.
        val validatorJSONCaptured = everyJSONValidatorConversionReturnsList(emptyList())

        pocketVideoStore.load()

        assertEquals(expectedBundledJSON, validatorJSONCaptured.captured)
    }

    // shared pref videos is copied into validator
    @Test
    fun `GIVEN shared preferences is not empty WHEN loading THEN the json from shared prefs is passed verbatim into the json validator`() {
        everyAssetManagerOpensInputStream("assets".toInputStream())
        val expectedSharedPrefsValue = "{ }"
        setJSONInSharedPrefs(expectedSharedPrefsValue)

        // The return value doesn't matter.
        val validatorJSONCaptured = everyJSONValidatorConversionReturnsList(emptyList())

        pocketVideoStore.load()

        assertEquals(expectedSharedPrefsValue, validatorJSONCaptured.captured)
    }

    @Test
    fun `GIVEN shared preferences is not empty WHEN loading THEN the converted videos are returned`() {
        everyAssetManagerOpensInputStream("assets".toInputStream())
        setJSONInSharedPrefs("{ }")
        everyJSONValidatorConversionReturnsList(POCKET_FEED_TEST_DATA)

        val returnVal = pocketVideoStore.load()

        assertEquals(POCKET_FEED_TEST_DATA, returnVal)
    }

    @Test
    fun `GIVEN shared preferences is empty WHEN the converted videos is null THEN loading returns an empty list`() {
        everyAssetManagerOpensInputStream("".toInputStream())
        everyJSONValidatorConversionReturnsList(null)
        val returnVal = pocketVideoStore.load()
        assertEquals(emptyList<PocketViewModel.FeedItem>(), returnVal)
    }

    @Test
    fun `GIVEN shared preferences is empty and conversion returns null WHEN loading THEN the input stream gets closed`() {
        verifyLoadInputStreamIsClosed(given = {
            everyJSONValidatorConversionReturnsList(null)
        })
    }

    @Test
    fun `GIVEN shared preferences is empty and conversion does not return null WHEN loading THEN the input stream gets closed`() {
        verifyLoadInputStreamIsClosed(given = {
            everyJSONValidatorConversionReturnsList(POCKET_FEED_TEST_DATA)
        })
    }

    @Test(expected = IOException::class)
    fun `GIVEN shared preferences is empty WHEN opening asset manager throws an exception THEN load throws the exception`() {
        every { assetManager.open(any()) } throws IOException()
        pocketVideoStore.load()
    }

    @Test(expected = IOException::class)
    fun `GIVEN shared preferences is empty WHEN read text throws an exception THEN load throws the exception`() {
        val inputStream = mockk<InputStream>().also {
            every { it.read(any(), any(), any()) } throws IOException()
        }

        everyAssetManagerOpensInputStream(inputStream)
        pocketVideoStore.load()
    }

    private fun verifyLoadInputStreamIsClosed(given: (InputStream) -> Unit) {
        val inputStream = spyk("assets".toInputStream())
        everyAssetManagerOpensInputStream(inputStream)
        given(inputStream)

        pocketVideoStore.load()

        verify { inputStream.close() }
    }

    private fun setJSONInSharedPrefs(json: String) {
        sharedPrefs.edit().putString(PocketVideoStore.KEY_VIDEO_JSON, json).apply()
    }

    private fun everyAssetManagerOpensInputStream(inputStream: InputStream) {
        every { assetManager.open(any()) } returns inputStream
    }

    private fun everyJSONValidatorConversionReturnsList(pocketVideoList: List<PocketViewModel.FeedItem.Video>?): CapturingSlot<String> {
        val validatorJSONCaptured = slot<String>()
        every { jsonValidator.convertJSONToPocketVideos(capture(validatorJSONCaptured)) } returns pocketVideoList
        return validatorJSONCaptured
    }

    private fun String.toInputStream() = ByteArrayInputStream(this.toByteArray())
}
