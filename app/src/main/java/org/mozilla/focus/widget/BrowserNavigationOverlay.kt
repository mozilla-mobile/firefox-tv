/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.browser_overlay.view.*
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings

enum class NavigationEvent {
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD, TURBO, RELOAD_YT, PIN_ACTION;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.navButtonHome -> HOME
            R.id.navButtonSettings -> SETTINGS
            R.id.turboButton -> TURBO
            R.id.pinButton -> PIN_ACTION
            else -> null
        }

        const val VAL_CHECKED = "checked"
        const val VAL_UNCHECKED = "unchecked"
    }
}

class BrowserNavigationOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle), View.OnClickListener {

    interface BrowserNavigationStateProvider {
        fun isBackEnabled(): Boolean
        fun isForwardEnabled(): Boolean
        fun getCurrentUrl(): String?
        fun isURLPinned(): Boolean
    }

    var onNavigationEvent: ((event: NavigationEvent, value: String?,
                             autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?) -> Unit)? = null

    var navigationStateProvider: BrowserNavigationStateProvider? = null

    var isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(makeVisible) {
            visibility = if (makeVisible) VISIBLE else GONE
            if (makeVisible) {
                navUrlInput.requestFocus()
                updateNavigationButtons()
                navUrlInput.setText(navigationStateProvider?.getCurrentUrl())
            } else {
                // #393: Youtube doesn't refocus properly, so refresh
                if (navUrlInput.text.contains("youtube.com/tv")) {
                    onNavigationEvent?.invoke(NavigationEvent.RELOAD_YT, null, null)
                }
            }
        }

    private var isTurboEnabled: Boolean
        get() = Settings.getInstance(context).isBlockingEnabled
        set(value) {
            Settings.getInstance(context).isBlockingEnabled = value
        }

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)
        listOf(navButtonBack, navButtonForward, navButtonReload, navButtonHome, navButtonSettings,
                turboButton, pinButton)
                .forEach {
                    it.setOnClickListener(this)
                }

        setupUrlInput()
        turboButton.isChecked = Settings.getInstance(context).isBlockingEnabled
        navButtonSettings.setImageResource(R.drawable.ic_settings)

        val tintDrawable: (Drawable?) -> Unit = { it?.setTint(ContextCompat.getColor(context, R.color.tv_white)) }
        navCloseHint.compoundDrawablesRelative.forEach(tintDrawable)
        navUrlInput.compoundDrawablesRelative.forEach(tintDrawable)
    }

    private fun setupUrlInput() = with (navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput.isNotEmpty()) {
                val cachedAutocompleteResult = lastAutocompleteResult // setText clears the reference so we cache it here.
                setText(cachedAutocompleteResult.text)
                onNavigationEvent?.invoke(NavigationEvent.LOAD, userInput, cachedAutocompleteResult)
            }
        }
        val autocompleteFilter = UrlAutoCompleteFilter()
        autocompleteFilter.load(context.applicationContext)
        setOnFilterListener { searchText, view -> autocompleteFilter.onFilter(searchText, view) }

        setOnBackPressedListener {
            if (isVisible) {
                requestFocus()
            }
        }
    }

    override fun onClick(view: View?) {
        val event = NavigationEvent.fromViewClick(view?.id) ?: return
        var value: String? = null

        val isTurboButtonChecked = turboButton.isChecked
        val isPinButtonChecked = pinButton.isChecked
        when (event) {
            NavigationEvent.TURBO -> {
                isTurboEnabled = isTurboButtonChecked
            }
            NavigationEvent.PIN_ACTION -> {
                value = if (isPinButtonChecked) NavigationEvent.VAL_CHECKED
                else NavigationEvent.VAL_UNCHECKED
            }
        }
        onNavigationEvent?.invoke(event, value, null)
        TelemetryWrapper.overlayClickEvent(event, isTurboButtonChecked, isPinButtonChecked)
    }

    fun updateNavigationButtons() {
        val canGoBack = navigationStateProvider?.isBackEnabled() ?: false
        navButtonBack.isEnabled = canGoBack
        navButtonBack.isFocusable = canGoBack
        navButtonBack.alpha = if (canGoBack) 1.0f else 0.3f

        val canGoForward = navigationStateProvider?.isForwardEnabled() ?: false
        navButtonForward.isEnabled = canGoForward
        navButtonForward.isFocusable = canGoForward
        navButtonForward.alpha = if (canGoForward) 1.0f else 0.3f

        pinButton.isChecked = navigationStateProvider?.isURLPinned() ?: false

        // Prevent the focus from looping to the bottom row when reaching the last
        // focusable element in the top row
        navButtonReload.nextFocusLeftId = when {
            canGoForward -> R.id.navButtonForward
            canGoBack -> R.id.navButtonBack
            else -> R.id.navButtonReload
        }
        navButtonForward.nextFocusLeftId = when {
            canGoBack -> R.id.navButtonBack
            else -> R.id.navButtonForward
        }

        // #548: Prevent the URL from changing while it has focus
        // so that user input isn't interrupted
        if (!navUrlInput.hasFocus()) {
            navUrlInput.setText(navigationStateProvider?.getCurrentUrl())
        }

        if (findFocus() == null) {
            requestFocus()
        }
    }
}
