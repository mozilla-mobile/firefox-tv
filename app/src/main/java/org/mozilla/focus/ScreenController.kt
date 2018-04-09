/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.home.HomeFragment
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.widget.InlineAutocompleteEditText

object ScreenController {
    /**
     * Loads the given url. If isTextInput is true, there should be no null parameters.
     */
    fun onUrlEnteredInner(context: Context, fragmentManager: FragmentManager,
                          urlStr: String,
                          isTextInput: Boolean,
                          autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?, inputLocation: UrlTextInputLocation?) {
        if (TextUtils.isEmpty(urlStr.trim())) {
            return
        }

        val isUrl = UrlUtils.isUrl(urlStr)
        val updatedUrlStr: String
        val searchTerms: String?
        if (isUrl) {
            updatedUrlStr = UrlUtils.normalize(urlStr)
            searchTerms = null
        } else {
            updatedUrlStr = UrlUtils.createSearchUrl(context, urlStr)
            searchTerms = urlStr.trim()
        }

        showBrowserScreenForUrl(fragmentManager, updatedUrlStr)

        val isSearch = !TextUtils.isEmpty(searchTerms)
        if (isTextInput) {
            // Non-text input events are handled at the source, e.g. home tile click events.
            if (autocompleteResult == null) {
                throw IllegalArgumentException("Expected non-null autocomplete result for text input")
            }
            if (inputLocation == null) {
                throw IllegalArgumentException("Expected non-null input location for text input")
            }

            TelemetryWrapper.urlBarEvent(!isSearch, autocompleteResult, inputLocation)
        }
    }

    fun showHomeScreen(fragmentManager: FragmentManager, onUrlEnteredListener: OnUrlEnteredListener) {
        val homeFragment = fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG) as HomeFragment?
        if (homeFragment != null && homeFragment.isVisible) {
            // This is already at the top of the stack - do nothing.
            return
        }

        // We don't want to be able to go back from the back stack, so clear the whole fragment back stack.
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Show the home screen.
        val newHomeFragment = HomeFragment.create()
        newHomeFragment.onSettingsPressed = { showSettingsScreen(fragmentManager) }
        newHomeFragment.onUrlEnteredListener = onUrlEnteredListener
        fragmentManager.beginTransaction()
                .replace(R.id.container, newHomeFragment, HomeFragment.FRAGMENT_TAG)
                .commit()
    }

    fun showSettingsScreen(fragmentManager: FragmentManager) {
        val settingsFragment = SettingsFragment.create()
        fragmentManager.beginTransaction()
                .replace(R.id.container, settingsFragment, SettingsFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    fun showBrowserScreenForCurrentSession(fragmentManager: FragmentManager, sessionManager: SessionManager) {
        val currentSession = sessionManager.currentSession

        val fragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?
        if (fragment != null && fragment.session.isSameAs(currentSession)) {
            // There's already a BrowserFragment displaying this session.
            return
        }

        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    fun showBrowserScreenForUrl(fragmentManager: FragmentManager, url: String) {
        // This code is not correct:
        // - We only support one session but it creates a new session when there's no BrowserFragment
        // such as each time we open a URL from the home screen.
        // - It doesn't handle the case where the BrowserFragment is non-null but not
        // visible: this can happen when a BrowserFragment is in the back stack, e.g. if this
        // method is called from Settings.
        //
        // However, from a user perspective, the behavior is correct (e.g. back stack functions
        // correctly with multiple sessions).
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as? BrowserFragment
        if (browserFragment != null && browserFragment.isVisible) {
            // We can't call loadUrl on the Fragment until the view hierarchy is inflated so we check
            // for visibility in addition to existence.
            browserFragment.loadUrl(url)
        } else {
            SessionManager.getInstance().createSession(Source.USER_ENTERED, url)
        }
    }
}
