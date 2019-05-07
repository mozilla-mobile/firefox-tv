/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.content.Context

const val MARGIN_START_OVERLAY_DP = 45 // Default start margin for Overlay in DP

fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
