package org.mozilla.focus.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.mozilla.focus.R

import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.content_onboarding.*

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        enable_turbo_mode.setOnClickListener { view ->
            
            finish()
        }

        disable_turbo_mode.setOnClickListener { view ->
            finish()
        }
    }
}
