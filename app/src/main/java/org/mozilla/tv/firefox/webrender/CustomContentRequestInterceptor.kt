/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.content.Context
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.tv.firefox.utils.URLs

/**
 * [RequestInterceptor] implementation to inject custom content for firefox:* pages.
 */
class CustomContentRequestInterceptor(
    private val context: Context
) : RequestInterceptor {

    private var currentPageURL = ""

    override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse.Content? {
        currentPageURL = uri

        return when (uri) {
            URLs.APP_URL_HOME, URLs.APP_URL_POCKET_ERROR ->
                RequestInterceptor.InterceptionResponse.Content("<html></html>")

            LocalizedContent.URL_ABOUT -> RequestInterceptor.InterceptionResponse.Content(
                LocalizedContent.generateAboutPage(context))

            else -> null
        }
    }

    override fun onErrorRequest(session: EngineSession, errorType: ErrorType, uri: String?): RequestInterceptor.ErrorResponse? {
        return uri?.let {
            val data = ErrorPage.loadErrorPage(context, uri, errorType)
            RequestInterceptor.ErrorResponse(data, uri)
        }
    }
}
