/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.search

import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.search.SearchEngineManager
import org.mozilla.tv.firefox.search.SearchEngineManagerFactory.AMAZON_SEARCH_CODE
import org.mozilla.tv.firefox.search.SearchEngineManagerFactory.AMAZON_SEARCH_CODE_US_ONLY
import org.mozilla.tv.firefox.utils.ServiceLocator

private val replacements = mapOf(
    "google" to AMAZON_SEARCH_CODE,
    "google-2018" to AMAZON_SEARCH_CODE_US_ONLY,
    "google-b-m" to AMAZON_SEARCH_CODE,
    "google-b-1-m" to AMAZON_SEARCH_CODE_US_ONLY
)
private val engineProvider = SearchEngineProviderWrapper(replacements)

/**
 * Encapsulates [SearchEngineManager] setup in order to clean up the
 * [ServiceLocator]
 */
object SearchEngineManagerFactory {

    val AMAZON_SEARCH_CODE = "google-b-amzftv"
    val AMAZON_SEARCH_CODE_US_ONLY = "google-b-1-amzftv"

    fun create(app: Application): SearchEngineManager {
        return SearchEngineManager(listOf(engineProvider)).apply {
            GlobalScope.launch {
                @Suppress("DeferredResultUnused")
                loadAsync(app) // Call is used only for its side effects
            }
        }
    }
}
