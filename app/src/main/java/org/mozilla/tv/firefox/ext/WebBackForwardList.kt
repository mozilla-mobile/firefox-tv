/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem

fun WebBackForwardList.toStandardList(): List<WebHistoryItem> {
    val size = this.size
    val list = mutableListOf<WebHistoryItem>()
    for (i in 0 until size) {
        list.add(getItemAtIndex(i))
    }
    return list
}
