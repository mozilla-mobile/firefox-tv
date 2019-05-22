/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import io.sentry.Sentry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.overlay_debug.debugLog
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.tv.firefox.components.locale.LocaleAwareAppCompatActivity
import org.mozilla.tv.firefox.ext.resetView
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.ext.setupForApp
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.onboarding.OnboardingActivity
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.telemetry.UrlTextInputLocation
import org.mozilla.tv.firefox.utils.BuildConstants
import org.mozilla.tv.firefox.utils.OnUrlEnteredListener
import org.mozilla.tv.firefox.utils.Settings
import org.mozilla.tv.firefox.utils.URLs
import org.mozilla.tv.firefox.utils.ViewUtils
import org.mozilla.tv.firefox.utils.publicsuffix.PublicSuffix
import org.mozilla.tv.firefox.webrender.VideoVoiceCommandMediaSession
import org.mozilla.tv.firefox.widget.InlineAutocompleteEditText

interface MediaSessionHolder {
    val videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession
}

class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener, MediaSessionHolder {
    private val LOG_TAG = "MainActivity"

    // There should be at most one MediaSession per process, hence it's in MainActivity.
    // We crash if we init MediaSession at init time, hence lateinit.
    override lateinit var videoVoiceCommandMediaSession: VideoVoiceCommandMediaSession

    override fun onCreate(savedInstanceState: Bundle?) {
        // We override onSaveInstanceState to not save state (for handling Clear Data), so startup flow
        // goes through onCreate.
        super.onCreate(savedInstanceState)

        PublicSuffix.init(this) // Used by Pocket Video feed & custom home tiles.
        initMediaSession()

        // The launch intent is needed to create the engines in the engine cache.
        val safeIntent = intent.toSafeIntent()
        webRenderComponents.notifyLaunchWithSafeIntent(safeIntent)

        arrayOf(
            serviceLocator.engineViewCache,
            serviceLocator.pocketVideoFetchScheduler
        ).forEach { lifecycle.addObserver(it) }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        val intentData = IntentValidator.validateOnCreate(this, safeIntent, savedInstanceState)

        val session = getOrCreateSession(intentData)

        webRenderComponents.sessionManager.getOrCreateEngineSession().resetView(this@MainActivity)

        val screenController = serviceLocator.screenController
        screenController.setUpFragmentsForNewSession(supportFragmentManager, session)

        serviceLocator.intentLiveData.observe(this, Observer {
            it?.consume {
                if (it != null) {
                    screenController.showBrowserScreenForUrl(supportFragmentManager, it.url)
                } else {
                    screenController.showBrowserScreenForCurrentSession(supportFragmentManager, session)
                }
                true
            }
        })

        if (!safeIntent.hasExtra("TURBO_MODE")) {
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

        serviceLocator.intentLiveData.value = Consumable.from(intentData)

        // Debug logging display for non public users
        // TODO: refactor out the debug variant visibility check in #1953
        BuildConstants.debugLogStr?.apply {
            debugLog.visibility = View.VISIBLE
            debugLog.text = this
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle?) {
        // Do not save instance state.
        //
        // onSaveInstanceState attempts to save view state, including fragments that we sometimes want destroyed. In
        // particular, this causes SettingsFragment to remain visible when it calls MainActivity#recreate in an attempt
        // to clear data.
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
        if (serviceLocator.sessionManager.selectedSession == null) {
            Sentry.capture(IllegalStateException("onNewIntent is called with null selectedSession"))
            return
        }

        // We can't do anything if the intent does not contain valid data, so short.
        val safeIntent = unsafeIntent.toSafeIntent()
        val intentData = IntentValidator.validate(this, safeIntent) ?: return

        /** ScreenController operations rely on Activity.LifeCycle (i.e. FragmentTransactions)
         *  Using LiveData allows such methods to be called in the correct LifeCycle */
        serviceLocator.intentLiveData.value = Consumable.from(intentData)

        if (safeIntent.hasExtra("TURBO_MODE")) {
            val turboMode = safeIntent.getBooleanExtra("TURBO_MODE", true)
            Log.i(LOG_TAG, "Setting turboMode.isEnabled = " + turboMode)
            serviceLocator.turboMode.isEnabled = turboMode
        }
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
        rootView.viewTreeObserver.addOnGlobalFocusChangeListener(serviceLocator.focusRepo)
    }

    override fun onStop() {
        super.onStop()
        // TODO when MainActivity has a VM, route this call through it
        serviceLocator.pocketRepo.stopBackgroundUpdates()
        TelemetryIntegration.INSTANCE.stopMainActivity()
        rootView.viewTreeObserver.removeOnGlobalFocusChangeListener(serviceLocator.focusRepo)
    }

    override fun onDestroy() {
        if (webRenderComponents.sessionManager.size > 0) {
            /**
             * This is to clear the previously assigned WebView instance from EngineView (which
             * uses ActivityContext) when it's destroyed via [EngineViewCache.onDestroy].
             *
             * webView instance is stored in the session, which means they'd stick around for the
             * life-time of the app. So we would need to manually deallocate the webView whenever
             * it's destroyed.
             *
             * Since [EngineSession.webView] is not nullable, we let [EngineSession.webView]
             * assign to a new bogus WebView instance (with ApplicationContext). This allows previously
             * assigned webView to be garbage collected and the newly assigned bogus webView to be replaced
             * in [MainActivity.onCreate]
             *
             * See [EngineSession.resetView] for additional context
             */
            webRenderComponents.sessionManager.getEngineSession()?.resetView(applicationContext)
        }
        super.onDestroy()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == EngineView::class.java.name) {
            context.serviceLocator.engineViewCache.getEngineView(context, attrs) {
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
        ViewUtils.hideKeyboard(container_navigation_overlay)
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, false,
                null, null)
    }

    override fun onTextInputUrlEntered(
        urlStr: String,
        autocompleteResult: InlineAutocompleteEditText.AutocompleteResult?,
        inputLocation: UrlTextInputLocation?
    ) {
        ViewUtils.hideKeyboard(container_navigation_overlay)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        serviceLocator.screenController.onUrlEnteredInner(this, supportFragmentManager, urlStr, true,
                autocompleteResult, inputLocation)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Back presses are all handled through onBackPressed.
        //
        // Note: on device, back presses emit one KEYCODE_BACK. On emulator, they
        // emit one KEYCODE_BACK **AND** one KEYCODE_DEL. We short on both to make
        // code paths consistent between the two.
        if (event.keyCode == KeyEvent.KEYCODE_BACK ||
                event.keyCode == KeyEvent.KEYCODE_DEL) return super.dispatchKeyEvent(event)

        val fragmentManager = supportFragmentManager

        TelemetryIntegration.INSTANCE.saveRemoteControlInformation(applicationContext, event)

        return videoVoiceCommandMediaSession.dispatchKeyEvent(event) ||
                serviceLocator.screenController.dispatchKeyEvent(event, fragmentManager) ||
                super.dispatchKeyEvent(event)
    }
}
