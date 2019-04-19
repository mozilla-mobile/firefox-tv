/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CheckResult

@CheckResult(suggest = "Recycle the MotionEvent after use")
fun KeyEvent.toMotionEvent(pos: PointF): MotionEvent =
    MotionEvent.obtain(this.downTime, this.eventTime, this.action, pos.x, pos.y, 0)
