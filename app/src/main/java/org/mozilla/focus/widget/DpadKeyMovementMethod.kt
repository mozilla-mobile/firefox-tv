/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.text.Selection
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.view.View
import android.widget.TextView

class DpadKeyMovementMethod : ArrowKeyMovementMethod() {

    override fun onTakeFocus(view: TextView?, text: Spannable?, dir: Int) {
        if (dir and (View.FOCUS_FORWARD or View.FOCUS_DOWN) != 0) {
            if (view?.layout == null) {
                // This shouldn't be null, but do something sensible if it is.
                Selection.setSelection(text, text!!.length)
            }
        }
    }
}
