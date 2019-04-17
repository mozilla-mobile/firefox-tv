/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import io.sentry.Sentry
import kotlinx.android.synthetic.main.settings_screen_buttons.view.*
import kotlinx.android.synthetic.main.settings_screen_switch.*
import kotlinx.android.synthetic.main.settings_screen_switch.view.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsScreen
import org.mozilla.tv.firefox.navigationoverlay.channels.SettingsTile
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.lang.IllegalStateException

const val KEY_SETTINGS_TYPE = "KEY_SETTINGS_TYPE"

/** The settings for the app. */
class SettingsFragment : Fragment() {
    enum class Action {
        SESSION_CLEARED
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val settingsVM = FirefoxViewModelProviders.of(this@SettingsFragment).get(SettingsViewModel::class.java)
        val type: SettingsTile = SettingsScreen.valueOf(arguments!!.getString(KEY_SETTINGS_TYPE)!!)
        val view = when (type) {
            SettingsScreen.DATA_COLLECTION -> setupDataCollectionScreen(inflater, container, settingsVM)
            SettingsScreen.CLEAR_COOKIES -> setupClearCookiesScreen(inflater, container, settingsVM)
            else -> {
                Sentry.capture(IllegalStateException("Unexpected Settings type received: $type"))
                return container!!
            }
        }
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }

        return view
    }

    private fun setupDataCollectionScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
    val view = inflater.inflate(R.layout.settings_screen_switch, parentView, false)
        settingsViewModel.dataCollectionEnabled.observe(viewLifecycleOwner, Observer<Boolean> { state ->
            view.toggle.isChecked = state ?: return@Observer
        })
        view.toggle.setOnClickListener {
            settingsViewModel.setDataCollectionEnabled(toggle.isChecked)
        }
        return view
    }

    private fun setupClearCookiesScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
        settingsViewModel.events.observe(viewLifecycleOwner, Observer {
            it?.consume { event ->
                when (event) {
                    Action.SESSION_CLEARED -> {
                        activity?.recreate()
                        TelemetryIntegration.INSTANCE.clearDataEvent()
                    }
                }.forceExhaustive
                true
            }
        })

        val view = inflater.inflate(R.layout.settings_screen_buttons, parentView, false)
        view.confirm_action.setOnClickListener {
            settingsViewModel.clearBrowsingData(serviceLocator!!.engineViewCache)
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }
        view.cancel_action.setOnClickListener {
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }
        return view
    }

    companion object {
        const val FRAGMENT_TAG = "settings"

        fun newInstance(type: SettingsScreen): SettingsFragment {
            return SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_SETTINGS_TYPE, type.toString())
                }
            }
        }
    }
}
