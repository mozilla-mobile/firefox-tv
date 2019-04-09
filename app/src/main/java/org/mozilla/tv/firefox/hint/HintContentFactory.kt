/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.hint

import android.content.res.Resources
import org.mozilla.tv.firefox.R

class HintContentFactory(private val resources: Resources) {

    fun getOpenMenuHint(): HintContent {
        val hint = resources.getString(R.string.hint_press_menu_to_open_overlay, resources.getString(R.string.firefox_tv_brand_name))
        val contentDescription =
                resources.getString(R.string.hint_press_menu_to_open_overlay_a11y, resources.getString(R.string.firefox_tv_brand_name))
        val drawableId = R.drawable.hardware_remote_menu
        return HintContent(hint, contentDescription, drawableId)
    }

    fun getCloseMenuHint(): HintContent {
        val hint = resources.getString(R.string.hint_press_back_to_close_overlay)
        val contentDescription = resources.getString(R.string.hint_press_back_to_close_overlay_a11y)
        val drawableId = R.drawable.hardware_remote_back
        return HintContent(hint, contentDescription, drawableId)
    }
}
