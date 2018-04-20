/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.view.View
import android.view.ViewGroup

val View.isVisible: Boolean get() = (visibility == View.VISIBLE)

/** Updates the layout params with the mutations provided to [mutateLayoutParams]. */
inline fun View.updateLayoutParams(mutateLayoutParams: (ViewGroup.LayoutParams) -> Unit) {
    val layoutParams = this.layoutParams
    mutateLayoutParams(layoutParams)
    this.layoutParams = layoutParams // Calling setLayoutParams forces them to refresh internally.
}
