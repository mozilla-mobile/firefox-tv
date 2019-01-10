/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

/**
 * Provides URL constants
 */
object URLs {
    private const val APP_URL_PREFIX = "firefox:"
    const val APP_URL_HOME = "${APP_URL_PREFIX}home"
    const val APP_URL_POCKET_ERROR = "${APP_URL_PREFIX}error:pocketconnection"
    const val YOUTUBE_TILE_URL = "https://ftv.cdn.mozilla.net/ytht"
    const val PRIVACY_NOTICE_URL = "https://www.mozilla.org/privacy/firefox-fire-tv/"
}
