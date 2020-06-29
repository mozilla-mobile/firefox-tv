/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.utils.BuildConstants.getInterceptionResponseContent
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
            URLs.APP_URL_HOME ->
                RequestInterceptor.InterceptionResponse.Content("<html></html>")

            URLs.URL_ABOUT -> getInterceptionResponseContent(
                LocalizedContent.generateAboutPage(context))

            URLs.URL_GPL -> getInterceptionResponseContent(
                LocalizedContent.generatePage(context, R.raw.gpl))

            URLs.URL_LICENSES -> {
                // Prevent getting stuck in this loop when clicking back from the activity
                Handler(Looper.getMainLooper()).post { context.serviceLocator.sessionRepo.attemptBack() }
                val intent = Intent(context, OssLicensesMenuActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(context, intent, null)

                null
            }

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
