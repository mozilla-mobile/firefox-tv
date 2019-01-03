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
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.UrlUtils
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

class ScreenController(private val stateMachine: ScreenControllerStateMachine) {

    // TODO cache overlay fragment so that custom pinned sites don't fade in every time the menu is opened

    /**
     * TODO
     *
     * not adding to backstack is intentional
     */
    fun setUpFragmentsForNewSession(fragmentManager: FragmentManager, session: Session) {
        val renderFragment = WebRenderFragment.createForSession(session)
        val pocketFragment = PocketVideoFragment()
        val settingsFragment = SettingsFragment()
        fragmentManager
            .beginTransaction()
            .add(R.id.container_settings, settingsFragment, SettingsFragment.FRAGMENT_TAG)
            .add(R.id.container_pocket, pocketFragment, PocketVideoFragment.FRAGMENT_TAG)
            .add(R.id.container_navigation_overlay, NavigationOverlayFragment(), NavigationOverlayFragment.FRAGMENT_TAG)
            .add(R.id.container_web_render,
                renderFragment, WebRenderFragment.FRAGMENT_TAG)
            .hide(renderFragment) // TODO note that this will need to be changed in order to display WebRenderFragment under a split overlay
            .hide(pocketFragment)
            .hide(settingsFragment)
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
        val settingsFragment = fragmentManager.settingsFragment()
        val overlayFragment = fragmentManager.navigationOverlayFragment()
        fragmentManager.beginTransaction()
            .show(settingsFragment)
            .hide(overlayFragment)
            .commit()
    }

    fun showPocketScreen(fragmentManager: FragmentManager) {
        val pocketFragment = fragmentManager.pocketFragment()
        val overlayFragment = fragmentManager.navigationOverlayFragment()

        fragmentManager.beginTransaction()
            .show(pocketFragment)
            .hide(overlayFragment)
            .commit()
    }

    fun showBrowserScreenForCurrentSession(fragmentManager: FragmentManager, session: Session) {
        if (session.url != URLs.APP_URL_HOME) {
            exposeWebRenderFragment(fragmentManager)
        }
    }

    private fun exposeWebRenderFragment(fragmentManager: FragmentManager) {
        val topFragments = listOf(
            fragmentManager.navigationOverlayFragment(),
            fragmentManager.pocketFragment(),
            fragmentManager.settingsFragment()
        )

        var transaction = fragmentManager.beginTransaction()

        topFragments.forEach {
            transaction = transaction.hide(it)
        }
        transaction.commit()
        stateMachine.webRenderLoaded()
    }

    fun showBrowserScreenForUrl(fragmentManager: FragmentManager, url: String) {
        // TODO comment explaining that browserfragment will always be available
        exposeWebRenderFragment(fragmentManager)
        stateMachine.webRenderLoaded()
        val browserFragment = fragmentManager.webRenderFragment()
        browserFragment.loadUrl(url)
    }

    // TODO: handle about:home and close overlay by button press separately
    fun showNavigationOverlay(fragmentManager: FragmentManager?, toShow: Boolean) {
        fragmentManager ?: return
        var transaction = fragmentManager.beginTransaction()
        val overlayFragment = fragmentManager.navigationOverlayFragment()
        val renderFragment = fragmentManager.webRenderFragment()
        if (toShow) {
            stateMachine.overlayOpened()
            transaction = transaction.show(overlayFragment)
                .hide(renderFragment) // TODO note that this will need to be changed in order to display WebRenderFragment under a split overlay
        } else {
            stateMachine.overlayClosed()
            transaction = transaction.hide(overlayFragment)
                .show(renderFragment)
        }
        transaction.commit()
    }

    fun handleBack(fragmentManager: FragmentManager): Boolean {
        println(fragmentManager)
        return false
    }

    fun handleMenu(fragmentManager: FragmentManager) {
        handleTransition(fragmentManager, stateMachine.menuPress())
    }

    private fun handleTransition(fragmentManager: FragmentManager, transition: ScreenControllerStateMachine.Transition) {
        when (transition) {
            ScreenControllerStateMachine.Transition.ADD_OVERLAY -> showNavigationOverlay(fragmentManager, true)
            ScreenControllerStateMachine.Transition.REMOVE_OVERLAY -> showNavigationOverlay(fragmentManager, false)
            ScreenControllerStateMachine.Transition.ADD_POCKET -> { /* TODO */ }
            ScreenControllerStateMachine.Transition.REMOVE_POCKET -> { /* TODO */}
            ScreenControllerStateMachine.Transition.ADD_SETTINGS -> { /* TODO */ }
            ScreenControllerStateMachine.Transition.REMOVE_SETTINGS -> { /* TODO */}
            ScreenControllerStateMachine.Transition.EXIT_APP -> { /* TODO */ }
            ScreenControllerStateMachine.Transition.NO_OP -> { }
        }
    }
}

private fun FragmentManager.webRenderFragment(): WebRenderFragment =
    this.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG) as WebRenderFragment

private fun FragmentManager.navigationOverlayFragment(): NavigationOverlayFragment =
    this.findFragmentByTag(NavigationOverlayFragment.FRAGMENT_TAG) as NavigationOverlayFragment

private fun FragmentManager.pocketFragment(): PocketVideoFragment =
    this.findFragmentByTag(PocketVideoFragment.FRAGMENT_TAG) as PocketVideoFragment

private fun FragmentManager.settingsFragment(): SettingsFragment =
    this.findFragmentByTag(SettingsFragment.FRAGMENT_TAG) as SettingsFragment
