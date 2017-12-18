/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_new_settings.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper

/** The home fragment which displays the navigation tiles of the app. */
class NewSettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_new_settings, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        telemetryButton.isChecked = TelemetryWrapper.isTelemetryEnabled(activity)
        telemetryView.setOnClickListener { view ->
            val newTelemetryState = !TelemetryWrapper.isTelemetryEnabled(activity)
            TelemetryWrapper.setTelemetryEnabled(activity, newTelemetryState)
            telemetryButton.isChecked = newTelemetryState
        }

        deleteButton.setOnClickListener { view ->
            val builder1 = AlertDialog.Builder(activity)
            builder1.setTitle(R.string.settings_cookies_dialog_title)
            builder1.setMessage(R.string.settings_cookies_dialog_content)
            builder1.setCancelable(true)

            builder1.setPositiveButton(
                    getString(R.string.action_ok),
                    DialogInterface.OnClickListener { dialog, id ->
                        SessionManager.getInstance().removeAllSessions()
                        settingsWebView.cleanup()
                        dialog.cancel()
                    })

            builder1.setNegativeButton(
                    getString(R.string.action_cancel),
                    DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })

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

    override fun onResume() {
        super.onResume()
        val a = activity
        if (a is MainActivity) {
            a.updateHintNavigationVisibility(MainActivity.VideoPlayerState.SETTINGS)
        }
    }

    companion object {
        const val FRAGMENT_TAG = "new_settings"

        @JvmStatic
        fun create() = NewSettingsFragment()
    }
}