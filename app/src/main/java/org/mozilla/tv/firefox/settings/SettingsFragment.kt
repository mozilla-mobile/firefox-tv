/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import kotlinx.android.synthetic.main.fragment_settings.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.deleteData
import org.mozilla.tv.firefox.ext.getAccessibilityManager
import org.mozilla.tv.firefox.ext.isVoiceViewEnabled
import org.mozilla.tv.firefox.ext.requireWebRenderComponents
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.telemetry.DataUploadPreference
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

/** The settings for the app. */
class SettingsFragment : Fragment() {
    private val voiceViewStateChangeListener = AccessibilityManager.TouchExplorationStateChangeListener {
        updateForAccessibility()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context

        ic_lock.setImageResource(R.drawable.mozac_ic_lock)
        telemetryButton.isChecked = DataUploadPreference.isEnabled(context)
        val dataPreferenceClickListener = { _: View ->
            val newTelemetryState = !DataUploadPreference.isEnabled(context)
            DataUploadPreference.setIsEnabled(context, newTelemetryState)
            telemetryButton.isChecked = newTelemetryState
        }
        // Due to accessibility hack for #293, where we want to focus a different (visible) element
        // for accessibility, either of these views could be unfocusable, so we need to set the
        // click listener on both.
        telemetryButtonContainer.setOnClickListener(dataPreferenceClickListener)
        telemetryButton.setOnClickListener(dataPreferenceClickListener)

        deleteButton.setOnClickListener { _ ->
            val builder1 = AlertDialog.Builder(activity)
            builder1.setTitle(R.string.settings_cookies_dialog_title)
            builder1.setMessage(R.string.settings_cookies_dialog_content2)
            builder1.setCancelable(true)

            builder1.setPositiveButton(
                getString(R.string.action_ok)
            ) { dialog, _ -> with(requireContext()) {
                    val components = requireWebRenderComponents
                    components.engine.deleteData(this)
                    components.sessionManager.removeAll()
                    dialog.cancel()
                    serviceLocator.webViewCache.doNotPersist()
                    // The call to recreate destroys state being maintained in the WebView (including
                    // navigation history) and Activity. This implementation will need to change
                    // if/when we add session restoration logic.
                    // See https://github.com/mozilla-mobile/firefox-tv/issues/1192
                    activity?.recreate()
                    TelemetryIntegration.INSTANCE.clearDataEvent()
                }
            }

            builder1.setNegativeButton(
                    getString(R.string.action_cancel),
                    { dialog, _ -> dialog.cancel() })

            val alert11 = builder1.create()
            alert11.show()
        }

        aboutButton.setOnClickListener {
            startActivity(InfoActivity.getAboutIntent(context))
        }

        privacyNoticeButton.setOnClickListener {
            startActivity(InfoActivity.getPrivacyNoticeIntent(context))
        }
    }

    override fun onStart() {
        super.onStart()
        context?.getAccessibilityManager()?.addTouchExplorationStateChangeListener(voiceViewStateChangeListener)
        updateForAccessibility()
    }

    override fun onStop() {
        super.onStop()
        context?.getAccessibilityManager()?.removeTouchExplorationStateChangeListener(voiceViewStateChangeListener)
    }

    /**
     * Updates the views in this fragment based on Accessibility status.
     * See the comment at the declaration of these views in XML for more details.
     */
    private fun updateForAccessibility() {
        // In order to read Accessibility text for the Telemetry checkbox WITH checked state,
        // we need to focus the checkbox in VoiceView instead of the containing view.
        //
        // When we change VoiceView from enabled -> disabled and this setting is focused, focus is
        // cleared from this setting and nothing is selected. This is fine: the user can press
        // left-right to focus something else and it's an edge case that I don't think it is worth
        // adding code to fix.
        val shouldFocusButton = context?.isVoiceViewEnabled() ?: return
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
