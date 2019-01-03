/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.content.Context
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import mozilla.components.browser.session.Session
import org.mozilla.tv.firefox.webrender.WebRenderFragment
import org.mozilla.tv.firefox.navigationoverlay.NavigationOverlayFragment
import org.mozilla.tv.firefox.pocket.PocketVideoFragment
import org.mozilla.tv.firefox.settings.SettingsFragment
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.UrlUtils
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

class ScreenController {

    /**
     * TODO
     *
     * not adding to backstack is intentional
     */
    fun setUpFragmentsForNewSession(fragmentManager: FragmentManager, session: Session) {
        fragmentManager
            .beginTransaction()
            .add(R.id.container, NavigationOverlayFragment(), NavigationOverlayFragment.FRAGMENT_TAG)
            .add(R.id.container,
                WebRenderFragment.createForSession(session), WebRenderFragment.FRAGMENT_TAG)
            .commitNow()
    }

    /**
     * Loads the given url. If isTextInput is true, there should be no null parameters.
     */
    fun onUrlEnteredInner(
        context: Context,
        fragmentManager: FragmentManager,
        urlStr: String,
        isTextInput: Boolean,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
        inputLocation: UrlTextInputLocation?
    ) {
        if (TextUtils.isEmpty(urlStr.trim())) {
            return
        }

        val isUrl = UrlUtils.isUrl(urlStr)
        val updatedUrlStr = if (isUrl) UrlUtils.normalize(urlStr) else UrlUtils.createSearchUrl(context, urlStr)

        showBrowserScreenForUrl(fragmentManager, updatedUrlStr)

        if (isTextInput) {
            // Non-text input events are handled at the source, e.g. home tile click events.
            if (autocompleteResult == null) {
                throw IllegalArgumentException("Expected non-null autocomplete result for text input")
            }
            if (inputLocation == null) {
                throw IllegalArgumentException("Expected non-null input location for text input")
            }

            TelemetryIntegration.INSTANCE.urlBarEvent(isUrl, autocompleteResult, inputLocation)
        }
    }

    fun showSettingsScreen(fragmentManager: FragmentManager) {
        val settingsFragment = SettingsFragment.create()
        fragmentManager.beginTransaction()
                .replace(R.id.container, settingsFragment, SettingsFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    fun showBrowserScreenForCurrentSession(fragmentManager: FragmentManager, session: Session) {
        val fragment = fragmentManager.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG) as WebRenderFragment?
        if (fragment != null && fragment.session == session) {
            // There's already a BrowserFragment displaying this session.
            return
        }

        // BrowserFragment is the base fragment, so don't add a backstack transaction.
//        fragmentManager TODO figure this out
//                .beginTransaction()
//                .replace(R.id.container,
//                        WebRenderFragment.createForSession(session), WebRenderFragment.FRAGMENT_TAG)
//                .commit()
    }

    fun showNavigationOverlay(fragmentManager: FragmentManager) {
        fragmentManager.beginTransaction()
                .replace(R.id.container, NavigationOverlayFragment(), NavigationOverlayFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    fun showBrowserScreenForUrl(fragmentManager: FragmentManager, url: String) {
        // TODO comment explaining that browserfragment will always be available
        val browserFragment = fragmentManager.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG) as? WebRenderFragment
        browserFragment!!.loadUrl(url)
    }

    fun showPocketScreen(fragmentManager: FragmentManager) {
        fragmentManager.beginTransaction()
                .replace(R.id.container, PocketVideoFragment(), PocketVideoFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
    }

    fun handleBack(): Boolean {
        return false
    }
}
