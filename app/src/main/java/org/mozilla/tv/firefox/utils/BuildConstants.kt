/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.tv.firefox.BuildConfig

/**
 * Provides constant values related to the build
 */
object BuildConstants {
    private const val BUILD_TYPE_DEBUG = "debug"
    private const val BUILD_TYPE_RELEASE = "release"
    private const val PRODUCT_FLAVOR_GECKO = "gecko"
    private val flavor = BuildFlavor()

    val isDevBuild: Boolean
        get() = BUILD_TYPE_DEBUG == BuildConfig.BUILD_TYPE

    val isReleaseBuild: Boolean
        get() = BUILD_TYPE_RELEASE == BuildConfig.BUILD_TYPE

    val isGeckoBuild: Boolean
        get() = PRODUCT_FLAVOR_GECKO == BuildConfig.FLAVOR

    val debugLogStr: String?
        get() = flavor.getDebugLogStr(isDevBuild)

    fun getEngineVersion(context: Context): String {
        return flavor.getEngineVersion(context)
    }

    fun getInterceptionResponseContent(content: String): RequestInterceptor.InterceptionResponse.Content {
        return flavor.getInterceptionResponseContent(content)
    }
}
