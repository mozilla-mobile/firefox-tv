/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.integration

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.search.SearchEngineManagerFactory
import org.mozilla.tv.firefox.search.SearchEngineProviderWrapper
import org.mozilla.tv.firefox.utils.UrlUtils

/**
 * This is an integration test between [SearchEngineManagerFactory], [SearchEngineProviderWrapper],
 * and A-C that ensures our search codes are not lost due to a change in any of those three places.
 */
class SearchEngineManagerFactoryTest {

    lateinit var app: FirefoxApplication

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun searchEnginesShouldIncludeFftvSearchCodes() {
        val searchEngineManager = SearchEngineManagerFactory.create(app)
        val searchEngines = searchEngineManager.getSearchEngines(app)
        val searchIds = searchEngines.map { it.identifier }

        assertTrue(searchIds.contains(SearchEngineManagerFactory.AMAZON_SEARCH_CODE))
        // We don't test for AMAZON_SEARCH_CODE_US_ONLY so that this test won't
        // fail when run outside of the US
    }

    @Test
    fun searchEngineUrlShouldIncludeFftvSearchCodes() {
        val searchUrl = UrlUtils.createSearchUrl(app, "cats")
        assertTrue(searchUrl.contains(SearchEngineManagerFactory.AMAZON_SEARCH_CODE) ||
                searchUrl.contains(SearchEngineManagerFactory.AMAZON_SEARCH_CODE_US_ONLY)
        )
    }
}
