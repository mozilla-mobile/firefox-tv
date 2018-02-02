/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.view.MotionEvent

/** Calls the block and then [MotionEvent.recycle]s this. Inspired by [AutoCloseable.use]. */
inline fun <R> MotionEvent.use(block: (MotionEvent) -> R): R {
    try {
        return block(this)
    } finally {
        recycle()
    }
}

