/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * Workaround that prevents focus loss in [RecyclerView]s that:
 * 1) have contents that go off the screen, and
 * 2) are operated on using a D-pad
 *
 * See https://issuetracker.google.com/issues/37067220 for [RecyclerView] bug
 * See https://stackoverflow.com/a/50956790/9307461 for suggested fix
 */
class FocusLossFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun clearFocus() {
        if (this.parent != null) {
            super.clearFocus()
        }
    }
}
