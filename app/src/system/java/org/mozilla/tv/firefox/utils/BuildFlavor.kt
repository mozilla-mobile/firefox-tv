/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.ext.webRenderComponents

/**
 * Provides build flavor specific tasks
 */
class BuildFlavor {
    fun getDebugLogStr(isDevBuild: Boolean): String? {
        if (isDevBuild) {
            return "DEBUG / " +
                    "FLAVOR: ${BuildConfig.FLAVOR} / " +
                    "VERSION: ${BuildConfig.VERSION_NAME}"
        }

        return null
    }

    fun getEngineVersion(context: Context): String {
        val userAgent = context.webRenderComponents.engine.settings.userAgentString
        // Fetch chromium version from user agent with regex (match Chrome + its predecessor
        // and end when one or more space) (i.e. "Chrome/{Version} {s}"
        val regex = "Chrome[^\\s]+".toRegex()
        val chromePrefixIndex = 7
        var chromeVersion = ""

        userAgent?.apply {
            chromeVersion = regex.find(this)?.value?.substring(chromePrefixIndex) ?: ""
        }

        return chromeVersion
    }

    fun getInterceptionResponseContent(localizedContent: String): RequestInterceptor.InterceptionResponse.Content {
        return RequestInterceptor.InterceptionResponse.Content(localizedContent)
    }
}
