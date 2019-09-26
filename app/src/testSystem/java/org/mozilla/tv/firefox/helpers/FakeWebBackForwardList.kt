/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.graphics.Bitmap
import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem

/**
 * An implementation of the [WebBackForwardList] abstract class for testing.
 */
class FakeWebBackForwardList(
    private val backingList: List<WebHistoryItem>,
    private val mockCurrentIndex: Int = 0
) : WebBackForwardList() {

    override fun getSize(): Int = backingList.size
    override fun getItemAtIndex(index: Int): WebHistoryItem = backingList[index]

    override fun getCurrentIndex(): Int = mockCurrentIndex
    override fun getCurrentItem(): WebHistoryItem? = backingList[mockCurrentIndex]

    override fun clone(): WebBackForwardList {
        TODO("not implemented: not needed yet")
    }
}

/**
 * An implementation of the [WebHistoryItem] abstract class for testing.
 */
class FakeWebHistoryItem(
    private val mockOriginalUrl: String? = null
) : WebHistoryItem() {

    override fun getOriginalUrl(): String? = mockOriginalUrl

    override fun getUrl(): String? {
        TODO("not implemented: not needed yet")
    }

    override fun getFavicon(): Bitmap? {
        TODO("not implemented: not needed yet")
    }

    override fun getTitle(): String? {
        TODO("not implemented: not needed yet")
    }

    override fun clone(): WebHistoryItem {
        TODO("not implemented: not needed yet")
    }
}

fun List<WebHistoryItem>.toFakeWebBackForwardList(): FakeWebBackForwardList {
    return FakeWebBackForwardList(this)
}
