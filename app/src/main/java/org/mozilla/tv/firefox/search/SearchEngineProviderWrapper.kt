/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.search

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.util.Locale
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.browser.search.provider.localization.SearchLocalization
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider
import mozilla.components.support.base.log.logger.Logger

private val logger = Logger("Search")

/**
 * Wraps an [AssetsSearchEngineProvider] to allow for replacements of
 * search plugins.
 *
 * @property replacements a map specifying which plugins to replace e.g.
 * mapOf("google" to "google-fftv") to replace google with google-fftv.
 */
class SearchEngineProviderWrapper(private val replacements: Map<String, String>) : SearchEngineProvider {

    val myLocalizationProvider = object : SearchLocalizationProvider {
        override suspend fun determineRegion() = SearchLocalization(
            language = Locale.getDefault().language,
            country = Locale.getDefault().country,
            region = Locale.getDefault().country
        )
    }

    private val inner = AssetsSearchEngineProvider(
        localizationProvider = myLocalizationProvider,
        additionalIdentifiers = replacements.values.toList()
    )

    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        val searchEngines = inner.loadSearchEngines(context)

        return updateSearchEngines(searchEngines, replacements)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun updateSearchEngines(searchEngines: SearchEngineList, replacements: Map<String, String>): SearchEngineList {
        var defaultSearch = searchEngines.default

        @Suppress("NAME_SHADOWING") // Defensive copy & mutable
        val searchEngines = mutableListOf<SearchEngine>().apply { addAll(searchEngines.list) }

        replacements.forEach { (old, new) ->
            if (defaultSearch?.identifier == old) {
                defaultSearch = searchEngines.firstOrNull { it.identifier == new } ?: defaultSearch
            }

            val newIndex = searchEngines.indexOfFirst { it.identifier == new }
            if (newIndex != -1) {
                val initialOldIndex = searchEngines.indexOfFirst { it.identifier == old }
                if (initialOldIndex != -1) {
                    val newEngine = searchEngines.removeAt(newIndex)
                    // index of old engine might have changed after removal
                    val oldIndex = searchEngines.indexOfFirst { it.identifier == old }
                    searchEngines[oldIndex] = newEngine
                } else {
                    logger.debug("Failed to replace plugin $old with $new")
                }
            } else {
                logger.debug("Failed to replace plugin $old with $new")
            }
        }
        return SearchEngineList(searchEngines, defaultSearch)
    }
}
