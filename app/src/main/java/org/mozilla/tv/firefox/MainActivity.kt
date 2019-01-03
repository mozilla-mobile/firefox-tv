/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.components.locale.LocaleAwareAppCompatActivity
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.setupForApp
import org.mozilla.tv.firefox.ext.toSafeIntent
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.onboarding.OnboardingActivity
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity
import org.mozilla.tv.firefox.telemetry.SentryIntegration
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.OnUrlEnteredListener
import org.mozilla.tv.firefox.utils.SafeIntent
import org.mozilla.tv.firefox.utils.Settings
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.utils.publicsuffix.PublicSuffix
import org.mozilla.tv.firefox.webrender.VideoVoiceCommandMediaSession
import org.mozilla.tv.firefox.webrender.WebRenderFragment
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

interface MediaSessionHolder {
    val videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession
}

class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener, MediaSessionHolder {

    // There should be at most one MediaSession per process, hence it's in MainActivity.
    // We crash if we init MediaSession at init time, hence lateinit.
    override lateinit var videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
        SentryIntegration.init(this)
        PublicSuffix.init(this) // Used by Pocket Video feed & custom home tiles.
        initMediaSession()
        lifecycle.addObserver(serviceLocator.webViewCache)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        val intent = SafeIntent(intent)
        val intentData = IntentValidator.validateOnCreate(this, intent, savedInstanceState)

        val session = getOrCreateSession(intentData)

        val screenController = serviceLocator.screenController
        screenController.setUpFragmentsForNewSession(supportFragmentManager, session)

        if (intentData != null) {
            screenController.showBrowserScreenForUrl(supportFragmentManager, intentData.url)
        } else {
            screenController.showBrowserScreenForCurrentSession(supportFragmentManager, session)
        }

        if (Settings.getInstance(this@MainActivity).shouldShowPocketOnboarding()) {
            val onboardingIntents =
                    Intent(this@MainActivity, PocketOnboardingActivity::class.java)
            startActivity(onboardingIntents)
        }

        if (Settings.getInstance(this@MainActivity).shouldShowTurboModeOnboarding()) {
            val onboardingIntent = Intent(this@MainActivity, OnboardingActivity::class.java)
            startActivity(onboardingIntent)
        }
    }

    /**
     * If a new [Session] is created, this also adds it to the SessionManager and selects it
     */
    private fun getOrCreateSession(intentData: ValidatedIntentData?): Session {
        return webRenderComponents.sessionManager.selectedSession
            ?: Session(
                initialUrl = intentData?.url ?: URLs.APP_URL_HOME,
                source = intentData?.source ?: Session.Source.NONE
            ).also { webRenderComponents.sessionManager.add(it, selected = true) }
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        // If no session is selected, onCreate has not yet been hit. Short and let onCreate take care of intent validation
        serviceLocator.sessionManager.selectedSession ?: return
        // We can't do anything if the intent does not contain valid data, so short
        val intentData = IntentValidator.validate(this, unsafeIntent.toSafeIntent()) ?: return
        serviceLocator.screenController.showBrowserScreenForUrl(supportFragmentManager, intentData.url)
    }

    override fun applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    override fun onResume() {
        super.onResume()
        TelemetryIntegration.INSTANCE.startSession(this)
    }

    override fun onPause() {
        super.onPause()
        TelemetryIntegration.INSTANCE.stopSession(this)
    }

    override fun onStart() {
        super.onStart()
        // TODO when MainActivity has a VM, route this call through it
        serviceLocator.pocketRepo.startBackgroundUpdates()
    }

    override fun onStop() {
        super.onStop()
        // TODO when MainActivity has a VM, route this call through it
        serviceLocator.pocketRepo.stopBackgroundUpdates()
        TelemetryIntegration.INSTANCE.stopMainActivity()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == EngineView::class.java.name) {
            context.serviceLocator.webViewCache.getWebView(context, attrs) {
                setupForApp()
            }
        } else super.onCreateView(name, context, attrs)
    }

    override fun onBackPressed() {
        if (serviceLocator.screenController.handleBack(supportFragmentManager)) return

        // If you're here that means there's nothing else in the fragment backstack; therefore, clear session
        webRenderComponents.sessionManager.remove()

        super.onBackPressed()
    }

    private fun initMediaSession() {
        videoVoiceCommandMediaSession = VideoVoiceCommandMediaSession(this)
        lifecycle.addObserver(videoVoiceCommandMediaSession)
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(container_parent)
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, false,
                null, null)
    }

    override fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
        inputLocation: UrlTextInputLocation?
    ) {
        ViewUtils.hideKeyboard(container_parent)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, true,
                autocompleteResult, inputLocation)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragmentManager = supportFragmentManager

        val maybeBrowserFragment = (fragmentManager.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG)
                as WebRenderFragment?)?.let {
            if (it.isVisible) it else null
        }

        if (event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                serviceLocator.screenController.handleMenu(supportFragmentManager)
            }
            return true
        }

        return videoVoiceCommandMediaSession.dispatchKeyEvent(event) ||
                (maybeBrowserFragment?.dispatchKeyEvent(event) ?: false) ||
                super.dispatchKeyEvent(event)
    }
}
