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

    const val FIREFOX_ACCOUNTS = "https://accounts.firefox.com"

    // Settings > About pages
    const val URL_ABOUT = "${APP_URL_PREFIX}about"
    const val URL_GPL = "${APP_URL_PREFIX}gpl"
    const val URL_LICENSES = "${APP_URL_PREFIX}licenses"
    const val PRIVACY_NOTICE_URL = "https://www.mozilla.org/privacy/firefox-fire-tv/"

    const val SEARCH_AMZN_MARKET_BY_PACKAGE = "amzn://apps/android?p="
}
