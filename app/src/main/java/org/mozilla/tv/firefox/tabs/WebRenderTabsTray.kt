/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.tabstray.TabsTray
import org.mozilla.tv.firefox.R

const val DEFAULT_ITEM_TEXT_COLOR = 0xFF111111.toInt()
const val DEFAULT_ITEM_TEXT_SELECTED_COLOR = 0xFFFFFFFF.toInt()

/**
 * Tabs tray for NavigationOverlay
 */
class WebRenderTabsTray @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val tabsAdapter: TabsAdapter = TabsAdapter()
) : RecyclerView(context, attrs, defStyleAttr),
    TabsTray by tabsAdapter {

    internal val styling: TabsTrayStyling

    init {
        tabsAdapter.tabsTray = this
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = tabsAdapter

        styling = TabsTrayStyling(
            context.getDrawable(R.drawable.tab_tile_background)!!,
            context.getDrawable(R.drawable.tab_tile_background_selected)!!,
            DEFAULT_ITEM_TEXT_COLOR,
            DEFAULT_ITEM_TEXT_SELECTED_COLOR,
            5f
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // Multiple tabs that we are currently displaying may be subscribed to a Session to update
        // automatically. Unsubscribe all of them now so that we do not reference them and keep them
        // in memory.
        tabsAdapter.unsubscribeHolders()
    }

    /**
     * Convenience method to cast this object to an Android View object.
     */
    override fun asView(): View {
        return this
    }
}

internal data class TabsTrayStyling(
    val itemBackground: Drawable,
    val selectedItemBackground: Drawable,
    val itemTextColor: Int,
    val selectedItemTextColor: Int,
    val itemElevation: Float
)
