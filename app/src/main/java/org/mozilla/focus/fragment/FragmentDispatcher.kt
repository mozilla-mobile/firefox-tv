/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.content.Context
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import org.mozilla.focus.R
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.widget.InlineAutocompleteEditText

class FragmentDispatcher {

    companion object {
        /**
         * Loads the given url. If isTextInput is true, there should be no null parameters.
         */
        fun onUrlEnteredInner(urlStr: String, isTextInput: Boolean,
                              autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
                              inputLocation: UrlTextInputLocation?,
                              fragmentManager: FragmentManager, sessionManager: SessionManager,
                              context: Context) {
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

            if (sessionManager.hasSession()) {
                sessionManager.currentSession.searchTerms = searchTerms // todo: correct?
            }

            // TODO: could this ever happen where browserFragment is on top? and do we need to do anything special for it?
            val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG)
            val isSearch = !TextUtils.isEmpty(searchTerms)
            if (browserFragment != null && browserFragment.isVisible) {
                // Reuse existing visible fragment - in this case we know the user is already browsing.
                // The fragment might exist if we "erased" a browsing session, hence we need to check
                // for visibility in addition to existence.
                (browserFragment as BrowserFragment).loadUrl(updatedUrlStr)

                // And this fragment can be removed again.
                fragmentManager.beginTransaction()
                        .replace(R.id.container, browserFragment)
                        .addToBackStack(null)
                        .commit()
            } else {
                if (isSearch) {
                    SessionManager.getInstance().createSearchSession(Source.USER_ENTERED, updatedUrlStr, searchTerms)
                } else {
                    SessionManager.getInstance().createSession(Source.USER_ENTERED, updatedUrlStr)
                }
            }

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
            // TODO: animations if fragment is found.
            val homeFragment = fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG) as HomeFragment?
            if (homeFragment != null && homeFragment.isVisible) {
                // This is already at the top of the stack - do nothing.
                return
            }

            // We don't want to be able to go back from the back stack, so clear the whole fragment back stack.
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            // Show the home screen.
            val newHomeFragment = HomeFragment.create()
            newHomeFragment.onUrlEnteredListener = onUrlEnteredListener
            fragmentManager.beginTransaction()
                    .replace(R.id.container, newHomeFragment, HomeFragment.FRAGMENT_TAG)
                    .commit()
        }

        fun showSettingsScreen(fragmentManager: FragmentManager) {
            // TODO: animations if fragment is found.
            val settingsFragment = NewSettingsFragment.create()
            fragmentManager.beginTransaction()
                    .replace(R.id.container, settingsFragment, NewSettingsFragment.FRAGMENT_TAG)
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
    }
}