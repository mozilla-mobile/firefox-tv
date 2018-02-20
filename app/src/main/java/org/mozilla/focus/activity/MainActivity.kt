/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager

import com.amazon.android.webkit.AmazonWebKitFactories
import com.amazon.android.webkit.AmazonWebKitFactory
import org.mozilla.focus.R
import org.mozilla.focus.architecture.NonNullObserver
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.FragmentDispatcher
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.IWebView
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.focus.widget.InlineAutocompleteEditText


class MainActivity : LocaleAwareAppCompatActivity(), OnUrlEnteredListener {

    private val sessionManager: SessionManager

    private var fragmentContainer: View? = null

    init {
        sessionManager = SessionManager.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initAmazonFactory()

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_main)

        fragmentContainer = findViewById(R.id.container)


        val intent = SafeIntent(intent)

        sessionManager.handleIntent(this, intent, savedInstanceState)

        sessionManager.sessions.observe(this, object : NonNullObserver<List<Session>>() {
            public override fun onValueChanged(sessions: List<Session>) {
                if (sessions.isEmpty()) {
                    // There's no active session. Show the URL input screen so that the user can
                    // start a new session.
                    FragmentDispatcher.showHomeScreen(supportFragmentManager, this@MainActivity)
                } else {
                    FragmentDispatcher.showBrowserScreenForCurrentSession(supportFragmentManager, sessionManager)
                }

                if (Settings.getInstance(this@MainActivity).shouldShowOnboarding()) {
                    val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                    startActivity(intent)
                }
            }
        })

        WebViewProvider.preload(this)
    }

    override fun applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    override fun onResume() {
        super.onResume()

        TelemetryWrapper.startSession()

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        super.onPause()
        TelemetryWrapper.stopSession()
    }

    override fun onStop() {
        super.onStop()
        TelemetryWrapper.stopMainActivity()
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        val intent = SafeIntent(unsafeIntent)

        sessionManager.handleNewIntent(this, intent)
    }


    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == IWebView::class.java.name) {
            // Inject our implementation of IWebView from the WebViewProvider.
            WebViewProvider.create(this, attrs, factory!!)
        } else super.onCreateView(name, context, attrs)

    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment
        if (browserFragment != null &&
                browserFragment.isVisible &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return
        }
        super.onBackPressed()
    }

    private fun initAmazonFactory() {
        if (!isAmazonFactoryInit) {
            factory = AmazonWebKitFactories.getDefaultFactory()
            if (factory!!.isRenderProcess(this)) {
                return  // Do nothing if this is on render process
            }
            factory!!.initialize(this.applicationContext)

            // factory configuration is done here, for example:
            factory!!.cookieManager.setAcceptCookie(true)

            isAmazonFactoryInit = true
        } else {
            factory = AmazonWebKitFactories.getDefaultFactory()
        }
    }

    override fun onNonTextInputUrlEntered(urlStr: String) {
        ViewUtils.hideKeyboard(fragmentContainer)
        FragmentDispatcher.onUrlEnteredInner(urlStr, false, null, null,
                supportFragmentManager, sessionManager, this)
    }

    override fun onTextInputUrlEntered(urlStr: String,
                                       autocompleteResult: InlineAutocompleteEditText.AutocompleteResult,
                                       inputLocation: UrlTextInputLocation) {

        ViewUtils.hideKeyboard(fragmentContainer)
        // It'd be much cleaner/safer to do this with a kotlin callback.
        FragmentDispatcher.onUrlEnteredInner(urlStr, true, autocompleteResult, inputLocation,
                supportFragmentManager, sessionManager, this)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragmentManager = supportFragmentManager
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment

        return if (browserFragment == null || !browserFragment.isVisible) {
            super.dispatchKeyEvent(event)
        } else browserFragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    }

    companion object {

        val EXTRA_TEXT_SELECTION = "text_selection"


        private var isAmazonFactoryInit = false
        var factory: AmazonWebKitFactory? = null
    }
}
