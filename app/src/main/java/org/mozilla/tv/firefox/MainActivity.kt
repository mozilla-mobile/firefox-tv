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
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.webrender.WebRenderFragment
import org.mozilla.tv.firefox.webrender.VideoVoiceCommandMediaSession
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.setupForApp
import org.mozilla.tv.firefox.ext.toSafeIntent
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity
import org.mozilla.tv.firefox.pocket.PocketVideoFragment
import org.mozilla.tv.firefox.components.locale.LocaleAwareAppCompatActivity
import org.mozilla.tv.firefox.onboarding.OnboardingActivity
import org.mozilla.tv.firefox.telemetry.SentryIntegration
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.OnUrlEnteredListener
import org.mozilla.tv.firefox.utils.SafeIntent
import org.mozilla.tv.firefox.utils.Settings
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.utils.publicsuffix.PublicSuffix
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

interface MediaSessionHolder {
    val videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession
}

class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener, MediaSessionHolder {

    // There should be at most one MediaSession per process, hence it's in MainActivity.
    // We crash if we init MediaSession at init time, hence lateinit.
    override lateinit var videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession

    private val sessionObserver by lazy {
        object : SessionManager.Observer {
            override fun onSessionSelected(session: Session) {
                serviceLocator.screenController.showBrowserScreenForCurrentSession(supportFragmentManager, session)
            }

            override fun onSessionRemoved(session: Session) {
                if (webRenderComponents.sessionManager.sessions.isEmpty()) {
                    onNoActiveSession()
                }
            }

            override fun onAllSessionsRemoved() {
                onNoActiveSession()
            }

            private fun onNoActiveSession() {
                // There's no active session. Start a new session with "homepage".
                serviceLocator.screenController.showBrowserScreenForUrl(
                    this@MainActivity,
                    supportFragmentManager,
                    URLs.APP_URL_HOME,
                    Session.Source.NONE
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
        SentryIntegration.init(this)
        PublicSuffix.init(this) // Used by Pocket Video feed & custom home tiles.
        initMediaSession()
        lifecycle.addObserver(serviceLocator.webViewCache)

        val intent = SafeIntent(intent)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        IntentValidator.validateOnCreate(this, intent, savedInstanceState, ::onValidBrowserIntent)

        webRenderComponents.sessionManager.register(sessionObserver, owner = this)

        if (webRenderComponents.sessionManager.sessions.isEmpty()) {
//            serviceLocator.screenController.showNavigationOverlay(supportFragmentManager) TODO
            serviceLocator.screenController.showBrowserScreenForUrl(
                this@MainActivity,
                supportFragmentManager,
                URLs.APP_URL_HOME,
                Session.Source.NONE
            )
        } else {
            serviceLocator.screenController.showBrowserScreenForCurrentSession(
                supportFragmentManager,
                webRenderComponents.sessionManager.selectedSessionOrThrow
            )
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

    override fun onNewIntent(unsafeIntent: Intent) {
        IntentValidator.validate(this, unsafeIntent.toSafeIntent(), ::onValidBrowserIntent)
    }

    private fun onValidBrowserIntent(url: String, source: Session.Source) {
        serviceLocator.screenController.showBrowserScreenForUrl(this, supportFragmentManager, url, source)
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
        // TODO: need new backstack implementation
//        val fragmentManager = supportFragmentManager
//        val browserFragment = fragmentManager.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG) as WebRenderFragment?
//        val settingsFragment = (fragmentManager.findFragmentByTag(SettingsFragment.FRAGMENT_TAG) as SettingsFragment?)?.let {
//            if (it.isVisible) it else null
//        }
//        val pocketFragment = (fragmentManager.findFragmentByTag(PocketVideoFragment.FRAGMENT_TAG) as PocketVideoFragment?)?.let {
//            if (it.isVisible) it else null
//        }
//
//        if (browserFragment != null) {
//            if (browserFragment.isVisible &&
//                    browserFragment.onBackPressed()) {
//                // The Browser fragment handles back presses on its own because it might just go back
//                // in the browsing history.
//                return
//            }
//
//            val currFragment =
//                    when {
//                        settingsFragment != null -> fragmentManager.findFragmentByTag(SettingsFragment.FRAGMENT_TAG)
//                        pocketFragment != null -> fragmentManager.findFragmentByTag(PocketVideoFragment.FRAGMENT_TAG)
//                        else -> null
//                    }
//
//            if (browserFragment.arguments == null) {
//                browserFragment.arguments = Bundle()
//            }
//
//            // Set ParentFragment flag to the BrowserFragment based on currFragment and let
//            // fragment lifecycle handle the rest
//            when (currFragment) {
//                is SettingsFragment -> {
//                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.SETTINGS)
//                }
//                is PocketVideoFragment -> {
//                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.POCKET)
//                }
//                else -> {
//                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.DEFAULT)
//                }
//            }
//        }

        super.onBackPressed()
    }

    private fun initMediaSession() {
        videoVoiceCommandMediaSession = VideoVoiceCommandMediaSession(this)
        lifecycle.addObserver(videoVoiceCommandMediaSession)
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(container)
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, false,
                null, null)
    }

    override fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
        inputLocation: UrlTextInputLocation?
    ) {
        ViewUtils.hideKeyboard(container)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, true,
                autocompleteResult, inputLocation)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragmentManager = supportFragmentManager
        val maybeBrowserFragment = (fragmentManager.findFragmentByTag(WebRenderFragment.FRAGMENT_TAG) as WebRenderFragment?)?.let {
            if (it.isVisible) it else null
        }
        val maybePocketFragment = (fragmentManager.findFragmentByTag(PocketVideoFragment.FRAGMENT_TAG) as PocketVideoFragment?)?.let {
            if (it.isVisible) it else null
        }
        return videoVoiceCommandMediaSession.dispatchKeyEvent(event) ||
                (maybePocketFragment?.dispatchKeyEvent(event) ?: false) ||
                (maybeBrowserFragment?.dispatchKeyEvent(event) ?: false) ||
                super.dispatchKeyEvent(event)
    }

    companion object {
        const val PARENT_FRAGMENT = "PARENT_FRAGMENT"
    }
}
