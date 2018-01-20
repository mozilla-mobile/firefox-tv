package org.mozilla.focus.activity

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import org.mozilla.focus.R

import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.content_onboarding.*
import org.mozilla.focus.webview.TrackingProtectionWebViewClient

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        enable_turbo_mode.setOnClickListener { _ ->
            setTurboMode(true)
            setOnboardShown()
            finish()
        }

        disable_turbo_mode.setOnClickListener { _ ->
            setTurboMode(false)
            setOnboardShown()
            finish()
        }
    }

    private fun setTurboMode(turboModeEnabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(TrackingProtectionWebViewClient.TRACKING_PROTECTION_ENABLED_PREF, turboModeEnabled)
                .apply();
    }

    private fun setOnboardShown() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(ONBOARD_PREF, true)
                .apply();
    }

    companion object {
        const val ONBOARD_PREF = "onboard_shown"
    }
}
