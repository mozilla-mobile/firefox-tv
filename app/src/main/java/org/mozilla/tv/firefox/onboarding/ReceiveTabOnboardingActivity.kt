/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.onboarding

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.receive_tab_onboarding.buttonNotNow
import kotlinx.android.synthetic.main.receive_tab_onboarding.buttonSignIn
import kotlinx.android.synthetic.main.receive_tab_onboarding.descriptionText
import org.mozilla.tv.firefox.R

class ReceiveTabOnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.receive_tab_onboarding)

        descriptionText.text = resources.getString(
            R.string.receive_tab_onboarding_description,
            resources.getString(R.string.firefox_tv_brand_name),
            resources.getString(R.string.firefox_tv_brand_name_short)
        )

        buttonSignIn.setOnClickListener {
            setResult(RESULT_SIGN_IN)
            finish()
        }

        buttonNotNow.setOnClickListener {
            finish()
        }

        setOnboardReceiveTabsShown()
    }

    private fun setOnboardReceiveTabsShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(ONBOARD_RECEIVE_TABS_SHOWN_PREF, true)
                .apply()
    }

    companion object {
        const val ONBOARD_RECEIVE_TABS_SHOWN_PREF = "onboard_receive_tabs_shown"
        const val RESULT_SIGN_IN = 1
    }
}
