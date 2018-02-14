/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
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
    HOME, SETTINGS, BACK, FORWARD, RELOAD, LOAD, TURBO, RELOAD_YT;

    companion object {
        fun fromViewClick(viewId: Int?) = when (viewId) {
            R.id.navButtonBack -> BACK
            R.id.navButtonForward -> FORWARD
            R.id.navButtonReload -> RELOAD
            R.id.navButtonHome -> HOME
            R.id.navButtonSettings -> SETTINGS
            R.id.turboButton -> TURBO
            else -> null
        }
    }
}

class BrowserNavigationOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0 )
    : LinearLayout(context, attrs, defStyle), View.OnClickListener {

    interface BrowserNavigationStateProvider {
        fun isBackEnabled(): Boolean
        fun isForwardEnabled(): Boolean
        fun getCurrentUrl(): String?
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
            turboButton.isActivated = value
            TelemetryWrapper.turboModeSwitchEvent(value)
        }

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.browser_overlay, this, true)
        listOf(navButtonBack, navButtonForward, navButtonReload, navButtonHome, navButtonSettings,
                turboButton)
                .forEach {
                    it.setOnClickListener(this)
                    DrawableCompat.setTintList(it.drawable.mutate(),
                            ContextCompat.getColorStateList(context, R.color.overlay_button_selector))
                    // Inactive state is used for Turbo mode
                    it.isActivated = true
                }
        setupUrlInput()
        turboButton.isActivated = Settings.getInstance(context).isBlockingEnabled
    }

    private fun setupUrlInput() = with (navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput.isNotEmpty()) {
                // getLastAutocompleteResult must be called before closeDrawer: closeDrawer clears the text input,
                // which clears the last autocomplete result.
                onNavigationEvent?.invoke(NavigationEvent.LOAD, userInput, lastAutocompleteResult)
                setText(lastAutocompleteResult.text)
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
        var event = NavigationEvent.fromViewClick(view?.id) ?: return
        if (event == NavigationEvent.TURBO) {
            isTurboEnabled = !turboButton.isActivated
            event = NavigationEvent.RELOAD
        }
        onNavigationEvent?.invoke(event, null, null)
    }

    fun setOverlayVisibleByUser(showOverlay: Boolean) {
        isVisible = showOverlay
        TelemetryWrapper.drawerShowHideEvent(showOverlay)
    }

    fun updateNavigationButtons() {
        val canGoBack = navigationStateProvider?.isBackEnabled() ?: false
        navButtonBack.isEnabled = canGoBack
        navButtonBack.isFocusable = canGoBack

        val canGoForward = navigationStateProvider?.isForwardEnabled() ?: false
        navButtonForward.isEnabled = canGoForward
        navButtonForward.isFocusable = canGoForward

        navUrlInput.setText(navigationStateProvider?.getCurrentUrl())

        if (findFocus() == null) {
            requestFocus()
        }
    }
}
