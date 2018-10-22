/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

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
import org.mozilla.focus.browser.BrowserFragment
import org.mozilla.focus.browser.BrowserFragment.Companion.APP_URL_HOME
import org.mozilla.focus.browser.BrowserNavigationOverlay
import org.mozilla.focus.browser.WebViewCache
import org.mozilla.focus.browser.VideoVoiceCommandMediaSession
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.serviceLocator
import org.mozilla.focus.ext.setupForApp
import org.mozilla.focus.ext.toSafeIntent
import org.mozilla.focus.home.pocket.PocketOnboardingActivity
import org.mozilla.focus.home.pocket.PocketVideoFragment
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.telemetry.SentryIntegration
import org.mozilla.focus.telemetry.TelemetryIntegration
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.utils.publicsuffix.PublicSuffix
import org.mozilla.focus.widget.InlineAutocompleteEditText

interface MediaSessionHolder {
    val videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession
}

class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener, MediaSessionHolder {

    // There should be at most one MediaSession per process, hence it's in MainActivity.
    // We crash if we init MediaSession at init time, hence lateinit.
    override lateinit var videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession
    private lateinit var webViewCache: WebViewCache

    private val sessionObserver = object : SessionManager.Observer {
        override fun onSessionSelected(session: Session) {
            ScreenController.showBrowserScreenForCurrentSession(supportFragmentManager, session)
        }

        override fun onSessionRemoved(session: Session) {
            if (components.sessionManager.sessions.isEmpty()) {
                onNoActiveSession()
            }
        }

        override fun onAllSessionsRemoved() {
            onNoActiveSession()
        }

        private fun onNoActiveSession() {
            // There's no active session. Start a new session with "homepage".
            ScreenController.showBrowserScreenForUrl(this@MainActivity, supportFragmentManager, APP_URL_HOME, Session.Source.NONE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
        SentryIntegration.init(this)
        serviceLocator.pocket.init()
        PublicSuffix.init(this) // Used by Pocket Video feed & custom home tiles.
        initMediaSession()
        initWebViewCache()

        val intent = SafeIntent(intent)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        IntentValidator.validateOnCreate(this, intent, savedInstanceState, ::onValidBrowserIntent)

        components.sessionManager.register(sessionObserver, owner = this)

        if (components.sessionManager.sessions.isEmpty()) {
            ScreenController.showBrowserScreenForUrl(this@MainActivity, supportFragmentManager, APP_URL_HOME, Session.Source.NONE)
        } else {
            ScreenController.showBrowserScreenForCurrentSession(
                supportFragmentManager,
                components.sessionManager.selectedSessionOrThrow)
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
        ScreenController.showBrowserScreenForUrl(this, supportFragmentManager, url, source)
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
        serviceLocator.pocket.startBackgroundUpdates()
    }

    override fun onStop() {
        super.onStop()
        serviceLocator.pocket.stopBackgroundUpdates() // Don't regularly hit the network in the background.
        TelemetryIntegration.INSTANCE.stopMainActivity()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == EngineView::class.java.name) {
            webViewCache.getWebView(context, attrs) {
                setupForApp()
            }
        } else super.onCreateView(name, context, attrs)
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

        if (browserFragment != null) {
            if (browserFragment.isVisible &&
                    browserFragment.onBackPressed()) {
                // The Browser fragment handles back presses on its own because it might just go back
                // in the browsing history.
                return
            }

            val currFragment = fragmentManager.findFragmentByTag(SettingsFragment.FRAGMENT_TAG)

            if (browserFragment.arguments == null) {
                browserFragment.arguments = Bundle()
            }

            // Set ParentFragment flag to the BrowserFragment based on currFragment and let
            // fragment lifecycle handle the rest
            when (currFragment) {
                is SettingsFragment -> {
                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.SETTINGS)
                }
                is PocketVideoFragment -> {
                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.POCKET)
                }
                else -> {
                    browserFragment.arguments!!.putSerializable(PARENT_FRAGMENT, BrowserNavigationOverlay.ParentFragment.DEFAULT)
                }
            }
        }

        super.onBackPressed()
    }

    private fun initMediaSession() {
        videoVoiceCommandMediaSession = VideoVoiceCommandMediaSession(this)
        lifecycle.addObserver(videoVoiceCommandMediaSession)
    }

    private fun initWebViewCache() {
        webViewCache = WebViewCache()
        lifecycle.addObserver(webViewCache)
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(container)
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, false,
                null, null)
    }

    override fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
        inputLocation: UrlTextInputLocation?
    ) {
        ViewUtils.hideKeyboard(container)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        ScreenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, true,
                autocompleteResult, inputLocation)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragmentManager = supportFragmentManager
        val maybeBrowserFragment = (fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?)?.let {
            if (it.isVisible) it else null
        }

        return videoVoiceCommandMediaSession.dispatchKeyEvent(event) ||
                (maybeBrowserFragment?.dispatchKeyEvent(event) ?: false) ||
                super.dispatchKeyEvent(event)
    }

    companion object {
        const val PARENT_FRAGMENT = "PARENT_FRAGMENT"
    }
}
