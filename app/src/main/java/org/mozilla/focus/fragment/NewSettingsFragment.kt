/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import org.mozilla.focus.telemetry.TelemetryWrapper
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.fragment_new_settings.*
import org.mozilla.focus.R
import android.R.string.cancel
import android.content.DialogInterface
import org.mozilla.focus.session.SessionManager


/** The home fragment which displays the navigation tiles of the app. */
class NewSettingsFragment : Fragment() {
    lateinit var telemetryButton: ImageView;
    lateinit var deleteButton: ImageView;
    lateinit var aboutButton: ImageView;

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?  {
        val rootView = inflater!!.inflate(R.layout.fragment_new_settings, container, false)
        telemetryButton = rootView.findViewById<ImageView>(R.id.telemetryButton)
        telemetryButton.setOnClickListener { view ->
            val telemetryEnabled = !TelemetryWrapper.isTelemetryEnabled(activity)
            TelemetryWrapper.setTelemetryEnabled(activity, telemetryEnabled)
            if (telemetryEnabled) {
                telemetryButton.setImageResource(R.drawable.ic_checkmark_enabled)
            } else {
                telemetryButton.setImageResource(R.drawable.ic_checkmark_disabled)
            }
        }

        deleteButton = rootView.findViewById(R.id.deleteButton)
        deleteButton.setOnClickListener { view ->
            val builder1 = AlertDialog.Builder(activity)
            builder1.setTitle(R.string.settings_cookies_dialog_title)
            builder1.setMessage(R.string.settings_cookies_dialog_content)
            builder1.setCancelable(true)

            builder1.setPositiveButton(
                    "OK",
                    DialogInterface.OnClickListener { dialog, id ->
                        SessionManager.getInstance().removeAllSessions()
                        dialog.cancel() })

            builder1.setNegativeButton(
                    "Cancel",
                    DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })

            val alert11 = builder1.create()
            alert11.show()
        }

        aboutButton = rootView.findViewById(R.id.aboutButton)
        aboutButton.setOnClickListener { view ->
            // Show new settings layout with text
        }
        return rootView
    }

    companion object {
        const val FRAGMENT_TAG = "new_settings"

        @JvmStatic
        fun create() = NewSettingsFragment()
    }
}