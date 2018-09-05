/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.widget.TextView

/**
 * An ArrowKeyMovementMethod that eliminates end cursor selection onFocus
 */
class DpadKeyMovementMethod : ArrowKeyMovementMethod() {
    override fun onTakeFocus(view: TextView, text: Spannable, dir: Int) {}
}
