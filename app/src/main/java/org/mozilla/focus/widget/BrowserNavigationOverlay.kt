/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.browser_overlay.view.*
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter

enum class NavigationEvent {
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.navButtonHome -> HOME
            R.id.navButtonSettings -> SETTINGS
            else -> null
        }
    }
}

class BrowserNavigationOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle), View.OnClickListener {

    interface NavigationEventHandler {
        fun onEvent(event: NavigationEvent, value: String? = null,
                    autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? = null)
    }

    private var eventHandler: NavigationEventHandler? = null

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)
        listOf(navButtonBack, navButtonForward, navButtonReload, navButtonHome, navButtonSettings)
                .forEach { it.setOnClickListener(this) }
        setupUrlInput()
    }

    private fun setupUrlInput() = with (navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput.isNotEmpty()) {
                // getLastAutocompleteResult must be called before closeDrawer: closeDrawer clears the text input,
                // which clears the last autocomplete result.
                eventHandler?.onEvent(NavigationEvent.LOAD, userInput, lastAutocompleteResult)
                setText(lastAutocompleteResult.text)
            }
        }
        val autocompleteFilter = UrlAutoCompleteFilter()
        autocompleteFilter.load(context.applicationContext)
        setOnFilterListener { searchText, view -> autocompleteFilter.onFilter(searchText, view) }

        setOnBackPressedListener {
            if (visibility == View.VISIBLE) {
                requestFocus()
            }
        }
    }

    override fun onClick(view: View?) {
        val event = NavigationEvent.fromViewClick(view?.id) ?: return
        eventHandler?.onEvent(event)
    }

    fun setNavigationEventHandler(handler: NavigationEventHandler) {
        eventHandler = handler
    }
}
