/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import io.reactivex.subjects.PublishSubject

// TODO find ANY other way to do this.  This is horrible
class ScrollBus {
    val scrollRequests = PublishSubject.create<Pair<Int, Int>>()
}