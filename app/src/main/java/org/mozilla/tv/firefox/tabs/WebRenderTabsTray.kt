/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.functions.Consumer
import mozilla.components.concept.tabstray.TabsTray
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.pocket.PocketViewModel

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
    val tabsUseCases = context.webRenderComponents.tabsUseCases

    private var focusDownId = -1
    val pocketStateObserver = Consumer<PocketViewModel.State> { state ->
        focusDownId = when (state) {
            is PocketViewModel.State.Error -> R.id.megaTileTryAgainButton
            is PocketViewModel.State.Feed -> R.id.pocketVideoMegaTileView
            else -> R.id.tileContainer
        }
    }

    init {
        tabsAdapter.tabsTray = this
        layoutManager = TabsTileLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = tabsAdapter

        styling = TabsTrayStyling(
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

    inner class TabsTileLayoutManager(
        context: Context,
        orientation: Int,
        reverseLayout: Boolean
    ) : LinearLayoutManager(context, orientation, reverseLayout) {
        override fun onRequestChildFocus(parent: RecyclerView, state: RecyclerView.State, child: View, focused: View?): Boolean {
            // TODO: move this to FocusRepo to eliminate tight coupling
            focused?.let {
                // if last element, then focus navUrlInput
                if (getPosition(it) == itemCount - 1)
                    it.nextFocusUpId = R.id.navUrlInput
                    it.nextFocusRightId = R.id.navUrlInput

                it.nextFocusDownId = focusDownId
            }

            return super.onRequestChildFocus(parent, state, child, focused)
        }
    }
}

internal data class TabsTrayStyling(
    val itemTextColor: Int,
    val selectedItemTextColor: Int,
    val itemElevation: Float
)
