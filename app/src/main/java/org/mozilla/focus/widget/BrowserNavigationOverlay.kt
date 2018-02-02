/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.browser_overlay.view.*
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.utils.ViewUtils

class BrowserNavigationOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle), View.OnClickListener {

    enum class NavigationEvent {
        HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD
    }

    interface NavigationEventHandler {
        fun onEvent(event: NavigationEvent, value: String? = null,
                    autocompleteResult: InlineAutocompleteEditText.AutocompleteResult? = null)
    }

    private var eventHandler: NavigationEventHandler? = null

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)
        listOf(navButtonBack, navButtonForward, navButtonReload, navButtonHome, navButtonSettings)
                .forEach { setOnClickListener(this) }
        setupUrlInput()
    }

    private fun setupUrlInput() {
        navUrlInput.imeOptions = navUrlInput.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING
        navUrlInput.setOnCommitListener {
            val userInput = navUrlInput.text.toString()
            if (!TextUtils.isEmpty(userInput)) {
                // getLastAutocompleteResult must be called before closeDrawer: closeDrawer clears the text input,
                // which clears the last autocomplete result.
                eventHandler?.onEvent(NavigationEvent.LOAD, userInput, navUrlInput.lastAutocompleteResult)
            }
        }
        val autocompleteFilter = UrlAutoCompleteFilter()
        autocompleteFilter.load(context.applicationContext)
        navUrlInput.setOnFilterListener { searchText, view -> autocompleteFilter.onFilter(searchText, view) }

        navUrlInput.setOnBackPressedListener {
            if (visibility == View.VISIBLE) {
                requestFocus()
                navUrlInput.requestFocus()
            }
        }
    }

    override fun onClick(view: View?) {
        val event = when (view?.id) {
            R.id.navButtonBack -> NavigationEvent.BACK
            R.id.navButtonForward -> NavigationEvent.FORWARD
            R.id.navButtonReload -> NavigationEvent.RELOAD
            R.id.navButtonHome -> NavigationEvent.HOME
            R.id.navButtonSettings -> NavigationEvent.SETTINGS
            else -> null
        }
        if (event != null) {
            eventHandler?.onEvent(event)
        }
    }

    fun setNavigationEventHandler(handler: NavigationEventHandler) {
        eventHandler = handler
    }
}
