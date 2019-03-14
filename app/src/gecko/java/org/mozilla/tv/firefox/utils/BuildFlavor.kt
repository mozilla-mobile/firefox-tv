/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.tv.firefox.BuildConfig

/**
 * Provides build flavor specific tasks
 */
class BuildFlavor {
    fun getDebugLogStr(isDevBuild: Boolean): String? {
        if (isDevBuild) {
            return "DEBUG / " +
                    "FLAVOR: ${BuildConfig.FLAVOR} / " +
                    "VERSION: ${BuildConfig.VERSION_NAME} / " +
                    "GECKO: ${BuildConfig.GECKOVIEW_VERSION}"
        }

        return null
    }

    /**
     * SystemWebView requires context to acquire version
     */
    fun getEngineVersion(@Suppress("UNUSED_PARAMETER") context: Context): String {
        return BuildConfig.GECKOVIEW_VERSION
    }

    /**
     * (This is a workaround for bug 1535131)
     * [GeckoEngineSession] calls GeckoSession.loadData() IFF encoding == "base64" (otherwise,
     * it calls GeckoSession.loadUri()). This is an expected behaviour from a-c since raw html
     * document is not base64 encoded. However, GeckoSession.loadUri() does not correctly load
     * the localized html content. So we have to pass in "base64" encoding field to
     * InterceptionResponse.Content for GeckoEngineSession to call GeckoSession.loadData()
     */
    fun getInterceptionResponseContent(localizedContent: String): RequestInterceptor.InterceptionResponse.Content {
        return RequestInterceptor.InterceptionResponse.Content(localizedContent, "text/html", "base64")
    }
}
