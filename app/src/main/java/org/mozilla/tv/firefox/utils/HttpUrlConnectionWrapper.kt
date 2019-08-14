/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient

/** A holder for the shared HttpClientConnection. */
object HttpUrlConnectionWrapper {
    val client = HttpURLConnectionClient()
}
