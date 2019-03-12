/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.search

import junit.framework.TestCase.assertEquals
import mozilla.components.browser.search.SearchEngine
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

private val GOOGLE = mockSearchEngine("Google")
private val YAHOO = mockSearchEngine("Yahoo")
private val GOOGLE_FFTV = mockSearchEngine("Google-FFTV")

class SearchEngineProviderWrapperTest {

    private lateinit var wrapper: SearchEngineProviderWrapper

    @Before
    fun setup() {
        wrapper = SearchEngineProviderWrapper(mapOf())
    }

    @Test
    fun `WHEN searchEngines sorting matches replacements sorting THEN values should replace as expected`() {
        val searchEngines = listOf(GOOGLE, YAHOO, GOOGLE_FFTV)
        val replacements = mapOf(GOOGLE.identifier to GOOGLE_FFTV.identifier)

        val expected = listOf(GOOGLE_FFTV, YAHOO)

        assertEquals(expected, wrapper.updateSearchEngines(searchEngines, replacements))
    }

    @Test
    fun `WHEN searchEngines is sorted reverse of replacements THEN values should replace as expected`() {
        val searchEngines = listOf(GOOGLE_FFTV, YAHOO, GOOGLE)
        val replacements = mapOf(GOOGLE.identifier to GOOGLE_FFTV.identifier)

        val expected = listOf(YAHOO, GOOGLE_FFTV)

        assertEquals(expected, wrapper.updateSearchEngines(searchEngines, replacements))
    }
}

private fun mockSearchEngine(id: String): SearchEngine {
    return mock(SearchEngine::class.java).also {
        `when`(it.identifier).thenReturn(id)
    }
}
