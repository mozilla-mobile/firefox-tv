/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.search

import android.app.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.search.SearchEngineManager
import org.mozilla.tv.firefox.utils.ServiceLocator

val replacements = mapOf("google" to "google-b-amzftv", "google-2018" to "google-b-1-amzftv",
        "google-b-m" to "google-b-amzftv", "google-b-1-m" to "google-b-1-amzftv")
val engineProvider = SearchEngineProviderWrapper(replacements)

/**
 * Encapsulates [SearchEngineManager] setup in order to clean up the
 * [ServiceLocator]
 */
object SearchEngineManagerFactory {
    fun create(app: Application): SearchEngineManager {
        return SearchEngineManager(listOf(engineProvider)).apply {
            GlobalScope.launch {
                @Suppress("DeferredResultUnused")
                load(app) // Call is used only for its side effects
            }
        }
    }
}
