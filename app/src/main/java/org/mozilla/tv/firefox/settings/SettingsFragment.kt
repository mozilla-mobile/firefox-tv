/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import android.app.AlertDialog
import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.architecture.FocusOnShowDelegate
import org.mozilla.tv.firefox.ext.forceExhaustive
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.URLs

/** The settings for the app. */
class SettingsFragment : Fragment() {
    enum class Action {
        SESSION_CLEARED
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        view.ic_lock.setImageResource(R.drawable.mozac_ic_lock)

        view.aboutButton.setOnClickListener {
            serviceLocator?.screenController?.showBrowserScreenForUrl(fragmentManager!!, URLs.URL_ABOUT)
        }
        view.privacyNoticeButton.setOnClickListener {
            serviceLocator?.screenController?.showBrowserScreenForUrl(fragmentManager!!, URLs.PRIVACY_NOTICE_URL)
        }

        FirefoxViewModelProviders.of(this@SettingsFragment).get(SettingsViewModel::class.java).also { settingsVM ->
            setupSettingsViewModel(view, settingsVM)
        }
        return view
    }

    override fun onHiddenChanged(hidden: Boolean) {
        FocusOnShowDelegate().onHiddenChanged(this, hidden)
        super.onHiddenChanged(hidden)
    }

    private fun setupSettingsViewModel(parentView: View, settingsViewModel: SettingsViewModel) {
        settingsViewModel.isVoiceViewEnabled.observe(viewLifecycleOwner, Observer<Boolean> {
            updateForAccessibility(it!!)
        })

        settingsViewModel.dataCollectionEnabled.observe(viewLifecycleOwner, Observer<Boolean> { state ->
            parentView.telemetryButton.isChecked = state ?: return@Observer
        })
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

        // Due to accessibility hack for #293, where we want to focus a different (visible) element
        // for accessibility, either of these views could be unfocusable, so we need to set the
        // click listener on both.
        parentView.telemetryButtonContainer.setOnClickListener {
            // Manually toggle the telemetry button from the container click
            telemetryButton.isChecked = !telemetryButton.isChecked
            settingsViewModel.setDataCollectionEnabled((telemetryButton.isChecked))
        }
        parentView.telemetryButton.setOnClickListener {
            settingsViewModel.setDataCollectionEnabled(telemetryButton.isChecked)
        }

        parentView.deleteButton.setOnClickListener { _ ->
            AlertDialog.Builder(activity)
                    .setTitle(R.string.settings_cookies_dialog_title)
                    .setMessage(R.string.settings_cookies_dialog_content2)
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                    with(requireContext()) {
                            settingsViewModel.clearBrowsingData(serviceLocator.engineViewCache)
                            dialog.cancel()
                        }
                    }
                    .setNegativeButton(
                            getString(R.string.action_cancel)) { dialog, _ -> dialog.cancel() }
                    .create().show()
        }
    }

    /**
     * Updates the views in this fragment based on Accessibility status.
     * See the comment at the declaration of these views in XML for more details.
     */
    private fun updateForAccessibility(isVoiceViewEnabled: Boolean) {
        // In order to read Accessibility text for the Telemetry checkbox WITH checked state,
        // we need to focus the checkbox in VoiceView instead of the containing view.
        //
        // When we change VoiceView from enabled -> disabled and this setting is focused, focus is
        // cleared from this setting and nothing is selected. This is fine: the user can press
        // left-right to focus something else and it's an edge case that I don't think it is worth
        // adding code to fix.
        val shouldFocusButton = isVoiceViewEnabled
        if (shouldFocusButton) {
            // Clear focus so that focus passes to child telemetryButton view.
            telemetryButtonContainer.isFocusable = false
            telemetryButtonContainer.clearFocus()
        } else {
            telemetryButtonContainer.isFocusable = true
        }
    }

    companion object {
        const val FRAGMENT_TAG = "settings"

        @JvmStatic
        fun create() = SettingsFragment()
    }
}
