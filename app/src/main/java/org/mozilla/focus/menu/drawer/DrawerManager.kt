/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.drawer

import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.text.TextUtils
import android.view.View
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.custom_drawer_item.view.*

import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.telemetry.MenuNavButton
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.widget.InlineAutocompleteEditText

class DrawerManager(drawerLayout: DrawerLayout, drawerUrlAutoCompleteFilter: UrlAutoCompleteFilter) {
    interface NavigationCallback {
        fun onNavigationEvent(button: MenuNavButton)
    }

    private val drawerLayout = drawerLayout.apply {
        addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(v: View?) {
                urlInput.requestFocus()
            }
        })
    }

    private val urlInput = drawerLayout.urlView

    private val backButton = drawerLayout.drawer_back_button
    private val forwardButton = drawerLayout.drawer_forward_button
    private val refreshButton = drawerLayout.drawer_refresh_button

    // TODO: set default value from sharedPrefs, update SharedPrefs on change
    private val trackingProtectionSwitch = drawerLayout.tracking_protection_switch.apply {
        setOnCheckedChangeListener { _, isChecked ->
            alpha = if (isChecked) 1f else .6f
        }
    }

    private var navigationCallback: NavigationCallback? = null

    init {
        drawerLayout.fragment_navigation.setNavigationItemSelectedListener { view ->
            when (view.itemId) {
                R.id.drawer_home -> sendBrowserAction(MenuNavButton.HOME)
                R.id.drawer_settings -> sendBrowserAction(MenuNavButton.SETTINGS)
                else -> false
            }
            true
        }

        setupUrlInput(urlInput, drawerUrlAutoCompleteFilter)

        val navClickListener = NavButtonClickListener()
        listOf(backButton, forwardButton, refreshButton).forEach {
            it.setOnClickListener(navClickListener)
        }
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

    inner class NavButtonClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val navAction = when (view?.id) {
                R.id.drawer_back_button -> MenuNavButton.BACK
                R.id.drawer_forward_button -> MenuNavButton.FORWARD
                R.id.drawer_refresh_button -> MenuNavButton.REFRESH
                else -> throw IllegalStateException("Unknown MenuNavButton")
            }
            sendBrowserAction(navAction)
        }
    }

    private fun sendBrowserAction(action: MenuNavButton) {
        navigationCallback?.onNavigationEvent(action)
        TelemetryWrapper.menuNavEvent(action)
        closeDrawerAutomatically()
    }

    fun setNavigationCallback(callback: NavigationCallback) {
        navigationCallback = callback
    }

    // TODO: Is there a better way to do this?
    fun turnOffTrackingProtectionFromOnboarding() {
        trackingProtectionSwitch.isChecked = false
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
