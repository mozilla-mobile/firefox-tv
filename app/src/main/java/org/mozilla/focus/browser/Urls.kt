/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

/**
 * A collection of known URLs and parts of URLs.
 */
object Urls {
    const val APP_HOME = "firefox:home"
    const val APP_PREFIX = "firefox:"
    const val DATA_PREFIX = "data:"
    const val APP_POCKET_ERROR = "${Urls.APP_PREFIX}error:pocketconnection"
}
