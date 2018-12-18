/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.webkit.WebBackForwardList

/**
 * Convert WebBackForwardList to List<String> so it is easier to use.
 */
fun WebBackForwardList.toStringList(): List<String> {
    return List(size) {
        getItemAtIndex(it).originalUrl
    }
}
