/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.engine

import android.content.Context
import android.webkit.WebViewClient
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.focus.browser.ErrorPage
import org.mozilla.focus.browser.LocalizedContent
import org.mozilla.focus.browser.Urls

const val ERROR_PROTOCOL = "error:"

/**
 * [RequestInterceptor] implementation to inject custom content for firefox:* pages.
 */
class CustomContentRequestInterceptor(
    private val context: Context
) : RequestInterceptor {

    private var currentPageURL = ""

    override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
        currentPageURL = uri

        return when (uri) {
            Urls.APP_HOME, Urls.APP_POCKET_ERROR -> RequestInterceptor.InterceptionResponse("<html></html>")

            LocalizedContent.URL_ABOUT -> RequestInterceptor.InterceptionResponse(
                LocalizedContent.generateAboutPage(context))

            else -> null
        }
    }

    @Suppress("NestedBlockDepth")
    override fun onErrorRequest(session: EngineSession, errorCode: Int, uri: String?) {
        uri?.let {
            // This is a hack: onReceivedError(WebView, WebResourceRequest, WebResourceError) is API 23+ only,
            // - the WebResourceRequest would let us know if the error affects the main frame or not. As a workaround
            // we just check whether the failing URL is the current URL, which is enough to detect an error
            // in the main frame.

            // WebView swallows odd pages and only sends an error (i.e. it doesn't go through the usual
            // shouldOverrideUrlLoading), so we need to handle special pages here:
            // about: urls are even more odd: webview doesn't tell us _anything_, hence the use of
            // a different prefix:
            if (it.startsWith(ERROR_PROTOCOL)) {
                // format: error:<error_code>
                val errorCodePosition = ERROR_PROTOCOL.length
                val errorCodeString = it.substring(errorCodePosition)

                var desiredErrorCode: Int
                try {
                    desiredErrorCode = Integer.parseInt(errorCodeString)

                    if (!ErrorPage.supportsErrorCode(desiredErrorCode)) {
                        // I don't think there's any good way of showing an error if there's an error
                        // in requesting an error page?
                        desiredErrorCode = WebViewClient.ERROR_BAD_URL
                    }
                } catch (e: NumberFormatException) {
                    desiredErrorCode = WebViewClient.ERROR_BAD_URL
                }
                val errorPageContent = ErrorPage.loadErrorPage(context, it, desiredErrorCode)
                session.loadData(errorPageContent)
            }
            // The API 23+ version also return a *slightly* more usable description, via WebResourceError.getError();
            // e.g.. "There was a network error.", whereas this version provides things like "net::ERR_NAME_NOT_RESOLVED"
            else if (it == currentPageURL && ErrorPage.supportsErrorCode(errorCode)) {
                val errorPageContent = ErrorPage.loadErrorPage(context, currentPageURL, errorCode)
                session.loadData(errorPageContent)
            }
        }
    }
}
