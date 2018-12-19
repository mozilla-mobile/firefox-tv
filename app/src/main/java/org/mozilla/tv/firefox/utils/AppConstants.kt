/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import org.mozilla.tv.firefox.BuildConfig

object AppConstants {
    private const val BUILD_TYPE_DEBUG = "debug"
    private const val BUILD_TYPE_RELEASE = "release"
    private const val APP_URL_PREFIX = "firefox:"
    const val APP_URL_HOME = "${APP_URL_PREFIX}home"
    const val APP_URL_POCKET_ERROR = "${APP_URL_PREFIX}error:pocketconnection"
    const val YOUTUBE_TILE_URL = "https://ftv.cdn.mozilla.net/ytht"

    val isDevBuild: Boolean
        get() = BUILD_TYPE_DEBUG == BuildConfig.BUILD_TYPE

    val isReleaseBuild: Boolean
        get() = BUILD_TYPE_RELEASE == BuildConfig.BUILD_TYPE
}
