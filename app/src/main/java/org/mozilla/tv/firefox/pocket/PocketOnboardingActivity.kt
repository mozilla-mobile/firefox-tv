/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pocket_onboarding.*
import org.mozilla.tv.firefox.R

class PocketOnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pocket_onboarding)

        titleText.text = resources.getString(R.string.pocket_home_tutorial_title,
                resources.getString(R.string.pocket_brand_name))
        descriptionText.text = resources.getString(R.string.pocket_home_tutorial_description,
                resources.getString(R.string.pocket_brand_name))

        pocket_onboarding_button.setOnClickListener { _ ->
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .edit()
                    .putBoolean(POCKET_ONBOARDING_SHOWN_PREF, true)
                    .apply()
            finish()
        }
    }

    companion object {
        const val POCKET_ONBOARDING_SHOWN_PREF = "pocket_onboarding_shown"
    }
}
