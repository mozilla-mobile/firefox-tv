/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_onboarding.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.serviceLocator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        setContent()

        enable_turbo_mode.setOnClickListener { _ ->
            setTurboMode(true)
            finish()
        }

        disable_turbo_mode.setOnClickListener { _ ->
            setTurboMode(false)
            setResult(Activity.RESULT_OK, Intent())
            finish()
        }

        setOnboardShown()
    }

    private fun setContent() {
        val content = serviceLocator.experimentsProvider.getTurboModeOnboarding()

        disable_turbo_mode.text = resources.getString(content.disableButtonTextId)
        enable_turbo_mode.text = resources.getString(content.enableButtonTextId)
        onboarding_main_text.text = resources.getString(content.descriptionId)
        turbo_mode_title.text = resources.getString(content.titleId)
        turbo_image_view.setImageResource(content.imageId)
        turbo_image_view.contentDescription = resources.getString(content.imageContentDescriptionId)
    }

    private fun setTurboMode(turboModeEnabled: Boolean) {
        serviceLocator.turboMode.isEnabled = turboModeEnabled
    }

    private fun setOnboardShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(ONBOARD_SHOWN_PREF, true)
                .apply()
    }

    companion object {
        const val ONBOARD_SHOWN_PREF = "onboard_shown"
    }
}
