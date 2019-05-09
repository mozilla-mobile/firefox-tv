/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.view.View
import org.mozilla.geckoview.GeckoView

private const val GECKO_VIEW_ID = 2147483645 // Int.MAX_VALUE - 2

/**
 * When view gains focus, its child(ren) views may gain focus with undefined View_ID
 * due to programmatic declaration
 */
fun View.validateKnownViewById(): Int {
    if (this.id == View.NO_ID) {
        when (this) {
            is GeckoView -> return GECKO_VIEW_ID
            else -> {
                // TODO: need sentry/telemetry to keep track of what views without IDs get passed in
            }
        }
    }

    return this.id
}

/**
 * Returns false if the view or any of its ancestors are not visible
 */
val View.isEffectivelyVisible: Boolean get() {
    var node: View? = this
    while (node != null) {
        if (node.visibility != View.VISIBLE) return false
        node = node.parent as? View
    }
    return true
}
