/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.view.View
import android.view.ViewGroup

val View.isVisible: Boolean get() = (visibility == View.VISIBLE)

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

/** Updates the layout params with the mutations provided to [mutateLayoutParams]. */
inline fun View.updateLayoutParams(mutateLayoutParams: (ViewGroup.LayoutParams) -> Unit) {
    val layoutParams = this.layoutParams
    mutateLayoutParams(layoutParams)
    this.layoutParams = layoutParams // Calling setLayoutParams forces them to refresh internally.
}
