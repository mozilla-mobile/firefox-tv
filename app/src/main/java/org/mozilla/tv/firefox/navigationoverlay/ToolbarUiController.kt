/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.support.v4.app.FragmentManager
import android.view.View
import android.widget.ImageButton
import kotlinx.android.synthetic.main.browser_overlay.view.*
import kotlinx.android.synthetic.main.browser_overlay_top_nav.view.*
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.support.ktx.android.view.forEach
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.widget.IgnoreFocusMovementMethod
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

private const val NAVIGATION_BUTTON_ENABLED_ALPHA = 1.0f
private const val NAVIGATION_BUTTON_DISABLED_ALPHA = 0.3f

/**
 * An encapsulation of the toolbar to set up and respond to UI operations.
 */
class ToolbarUiController(
    private val toolbarViewModel: ToolbarViewModel,
    private val exitFirefox: () -> Unit,
    private val updateFocusableViews: () -> Unit,
    private val onNavigationEvent: (NavigationEvent, String?, InlineAutocompleteEditText.AutocompleteResult?) -> Unit
) {

    private var hasUserChangedURLSinceEditTextFocused = false

    fun onCreateView(layout: View, viewLifecycleOwner: LifecycleOwner, fragmentManager: FragmentManager) {
        val toolbarClickListener = ToolbarOnClickListener()
        layout.topNavContainer.forEach {
            it.nextFocusDownId = layout.navUrlInput.id
            if (it.isFocusable) it.setOnClickListener(toolbarClickListener)
        }

        setupUrlInput(layout)
        initToolbar(layout, viewLifecycleOwner, fragmentManager)
    }

    private fun setupUrlInput(layout: View) = with(layout.navUrlInput) {
        setOnCommitListener {
            val userInput = text.toString()
            if (userInput == URLs.APP_URL_HOME) {
                // If the input points to home, we short circuit and hide the keyboard, returning
                // the user to the home screen
                this.hideKeyboard()
                return@setOnCommitListener
            }

            if (userInput.isNotEmpty()) {
                val cachedAutocompleteResult = lastAutocompleteResult // setText clears the reference so we cache it here.
                setText(cachedAutocompleteResult.text)
                onNavigationEvent.invoke(NavigationEvent.LOAD_URL, userInput, cachedAutocompleteResult)
            }
        }
        this.movementMethod = IgnoreFocusMovementMethod()
        val autocompleteProvider = ShippedDomainsProvider().apply {
            initialize(
                    context = context
            )
        }
        setOnFilterListener { searchText, view ->
            val result = autocompleteProvider.getAutocompleteSuggestion(searchText)
            if (result != null)
                view?.onAutocomplete(InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.totalItems))
        }

        setOnUserInputListener { hasUserChangedURLSinceEditTextFocused = true }
        setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) hasUserChangedURLSinceEditTextFocused = false }
    }

    private fun initToolbar(layout: View, viewLifecycleOwner: LifecycleOwner, fragmentManager: FragmentManager) {
        fun updateOverlayButtonState(isEnabled: Boolean, overlayButton: ImageButton) {
            overlayButton.isEnabled = isEnabled
            overlayButton.isFocusable = isEnabled
            overlayButton.alpha =
                    if (isEnabled) NAVIGATION_BUTTON_ENABLED_ALPHA else NAVIGATION_BUTTON_DISABLED_ALPHA
        }

        val context = layout.context
        val serviceLocator = context.serviceLocator

        toolbarViewModel.state.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer
            updateOverlayButtonState(it.backEnabled, layout.navButtonBack)
            updateOverlayButtonState(it.forwardEnabled, layout.navButtonForward)
            updateOverlayButtonState(it.pinEnabled, layout.pinButton)
            updateOverlayButtonState(it.refreshEnabled, layout.navButtonReload)
            updateOverlayButtonState(it.desktopModeEnabled, layout.desktopModeButton)

            updateFocusableViews()

            layout.pinButton.isChecked = it.pinChecked
            layout.desktopModeButton.isChecked = it.desktopModeChecked
            layout.turboButton.isChecked = it.turboChecked

            if (!hasUserChangedURLSinceEditTextFocused) {
                // The url can get updated in the background, e.g. if a loading page is redirected. We
                // don't want a url update to interrupt the user typing so we don't update the url from
                // the background if the user has already updated the url themselves.
                //
                // We revert this state when the view is unfocused: it ensures the URL is usually accurate
                // (for security reasons) and it's simple compared to other options which keep more state.
                //
                // One problem this solution has is that if the URL is updated in the background rapidly,
                // sometimes key events will be dropped, but I don't think there's much we can do about this:
                // we can't determine if the keyboard is up or not and focus isn't a good indicator because
                // we can focus the EditText without opening the soft keyboard and the user won't even know
                // these are inaccurate!
                layout.navUrlInput.setText(it.urlBarText)
            }
        })

        toolbarViewModel.events.observe(viewLifecycleOwner, Observer {
            it?.consume {
                when (it) {
                    is ToolbarViewModel.Action.ShowTopToast -> ViewUtils.showCenteredTopToast(context, it.textId)
                    is ToolbarViewModel.Action.ShowBottomToast -> ViewUtils.showCenteredBottomToast(context, it.textId)
                    is ToolbarViewModel.Action.SetOverlayVisible -> serviceLocator.screenController
                            .showNavigationOverlay(fragmentManager, it.visible)
                    ToolbarViewModel.Action.ExitFirefox -> exitFirefox()
                }.forceExhaustive
                true
            }
        })
    }

    private inner class ToolbarOnClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val event = NavigationEvent.fromViewClick(view?.id)
                    ?: return

            when (event) {
                NavigationEvent.BACK -> toolbarViewModel.backButtonClicked()
                NavigationEvent.FORWARD -> toolbarViewModel.forwardButtonClicked()
                NavigationEvent.RELOAD -> toolbarViewModel.reloadButtonClicked()
                NavigationEvent.PIN_ACTION -> toolbarViewModel.pinButtonClicked()
                NavigationEvent.TURBO -> toolbarViewModel.turboButtonClicked()
                NavigationEvent.DESKTOP_MODE -> toolbarViewModel.desktopModeButtonClicked()
                NavigationEvent.EXIT_FIREFOX -> toolbarViewModel.exitFirefoxButtonClicked()
                else -> Unit // Nothing to do.
            }
            onNavigationEvent.invoke(event, null, null)
        }
    }
}
