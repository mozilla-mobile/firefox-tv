/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.drawer

import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.text.TextUtils
import android.view.View
import kotlinx.android.synthetic.main.custom_drawer_item.view.*

import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.telemetry.MenuBrowserNavButton
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.widget.InlineAutocompleteEditText

class DrawerManager(drawerLayout: DrawerLayout, drawerUrlAutoCompleteFilter: UrlAutoCompleteFilter) {
    private val drawerLayout = drawerLayout.apply {
        addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(v: View?) {
                urlInput.requestFocus()
            }
        })
    }

    private val urlInput = drawerLayout.urlView.also {
        setupUrlInput(it, drawerUrlAutoCompleteFilter)
    }
    private val backButton = drawerLayout.drawer_back_button.apply { setOnClickListener(navigationClickListener) }
    private val forwardButton = drawerLayout.drawer_forward_button.apply { setOnClickListener(navigationClickListener) }
    private val refreshButton = drawerLayout.drawer_refresh_button.apply { setOnClickListener(navigationClickListener) }

    // TODO: set default value from sharedPrefs, update SharedPrefs on change
    private val trackingProtectionSwitch = drawerLayout.tracking_protection_switch.apply {
        setOnCheckedChangeListener { _, isChecked ->
            alpha = if (isChecked) 1f else .6f
        }
    }

    private val navigationClickListener = View.OnClickListener { view ->
        val navAction = when (view.id) {
            R.id.drawer_back_button -> MenuBrowserNavButton.BACK
            R.id.drawer_forward_button -> MenuBrowserNavButton.FORWARD
            R.id.drawer_refresh_button -> MenuBrowserNavButton.REFRESH
            else -> throw IllegalStateException("Unknown MenuBrowserNavButton")
        }
        sendBrowserAction(navAction)
    }

    private fun setupUrlInput(urlInputView: InlineAutocompleteEditText, drawerUrlAutoCompleteFilter: UrlAutoCompleteFilter) {
        urlInputView.imeOptions = urlInputView.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING
        urlInputView.setOnCommitListener({
            val userInput = urlInputView.text.toString()
            if (!TextUtils.isEmpty(userInput)) {
                // getLastAutocompleteResult must be called before closeDrawer: closeDrawer clears the text input,
                // which clears the last autocomplete result.
                // TODO: Send urlInput + autocomplete to MA
                // onTextInputUrlEntered(userInput, urlInput.getLastAutocompleteResult(), UrlTextInputLocation.MENU)
                closeDrawerAutomatically()
            }
        })
        urlInputView.setOnFilterListener({
            searchText, view -> drawerUrlAutoCompleteFilter.onFilter(searchText, view) })

        urlInputView.setOnBackPressedListener({
            // TODO: does this need to be only when drawer is open?
            drawerLayout.requestFocus()
            urlInput.requestFocus()
        })
    }

    // TODO: Is there a better way to do this?
    fun turnOffTrackingProtectionFromOnboarding() {
        trackingProtectionSwitch.isChecked = false
    }

    fun sendBrowserAction(action: MenuBrowserNavButton) {
        // TODO: Send event to MainActivity
        TelemetryWrapper.menuBrowserNavEvent(action)
        closeDrawerAutomatically()
    }

    fun toggleDrawerByUser() {
        val isDrawerOpen = drawerLayout.isDrawerOpen(GravityCompat.START)
        if (isDrawerOpen) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
            urlInput.requestFocus()
        }

        TelemetryWrapper.drawerShowHideEvent(!isDrawerOpen)
    }

    fun closeDrawerAutomatically() {
        drawerLayout.closeDrawer(GravityCompat.START)
        // TODO: notifiy BrowserFrag bc of Youtube freezing bug
    }

    fun updateDrawerUIState(url: String?, backEnabled: Boolean, forwardEnabled: Boolean, refreshEnabled: Boolean) {
        urlInput.setText(url)
        // TODO LATER: use bundled state instead of booleans
        forwardButton.isEnabled = forwardEnabled
        backButton.isEnabled = backEnabled
        refreshButton.isEnabled = refreshEnabled
        // TODO: set enabled state through selector
    }

    // TODO: Remove this as part of key handling chain
    fun isDrawerOpen(): Boolean {
        return drawerLayout.isDrawerOpen(GravityCompat.START)
    }


}
