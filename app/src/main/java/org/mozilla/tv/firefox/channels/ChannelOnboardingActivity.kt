/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.channel_onboarding.tv_onboarding_button
import org.mozilla.tv.firefox.R

class ChannelOnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.channel_onboarding)

        tv_onboarding_button.setOnClickListener { _ ->
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .edit()
                    .putBoolean(TV_ONBOARDING_SHOWN_PREF, true)
                    .apply()
            finish()
        }
    }

    companion object {
        const val TV_ONBOARDING_SHOWN_PREF = "tv_onboarding_shown"
    }
}
