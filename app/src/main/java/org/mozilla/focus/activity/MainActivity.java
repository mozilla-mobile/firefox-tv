/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.amazon.android.webkit.AmazonWebKitFactories;
import com.amazon.android.webkit.AmazonWebKitFactory;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.R;
import org.mozilla.focus.architecture.NonNullObserver;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FragmentDispatcher;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.session.Session;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.telemetry.UrlTextInputLocation;
import org.mozilla.focus.utils.OnUrlEnteredListener;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.InlineAutocompleteEditText;

import java.util.List;


public class MainActivity extends LocaleAwareAppCompatActivity implements OnUrlEnteredListener {

    public static final String EXTRA_TEXT_SELECTION = "text_selection";

    private final SessionManager sessionManager;

    private View fragmentContainer;

    public MainActivity() {
        sessionManager = SessionManager.getInstance();
    }


    private static boolean isAmazonFactoryInit = false;
    public static AmazonWebKitFactory factory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initAmazonFactory();

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.container);


        final SafeIntent intent = new SafeIntent(getIntent());

        sessionManager.handleIntent(this, intent, savedInstanceState);

        sessionManager.getSessions().observe(this,  new NonNullObserver<List<Session>>() {
            @Override
            public void onValueChanged(@NonNull List<Session> sessions) {
                if (sessions.isEmpty()) {
                    // There's no active session. Show the URL input screen so that the user can
                    // start a new session.
                    FragmentDispatcher.showHomeScreen(getSupportFragmentManager(), MainActivity.this);
                } else {
                    FragmentDispatcher.showBrowserScreenForCurrentSession(getSupportFragmentManager(), sessionManager);
                }

                if (Settings.getInstance(MainActivity.this).shouldShowOnboarding()) {
                    final Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                }
            }
        });

        WebViewProvider.preload(this);
    }

    @Override
    public void applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    @Override
    protected void onResume() {
        super.onResume();

        TelemetryWrapper.startSession();

        if (Settings.getInstance(this).shouldUseSecureMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TelemetryWrapper.stopSession();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TelemetryWrapper.stopMainActivity();
    }

    @Override
    protected void onNewIntent(Intent unsafeIntent) {
        final SafeIntent intent = new SafeIntent(unsafeIntent);

        sessionManager.handleNewIntent(this, intent);
    }


    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        if (name.equals(IWebView.class.getName())) {
            // Inject our implementation of IWebView from the WebViewProvider.
            return WebViewProvider.create(this, attrs, factory);
        }

        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (browserFragment != null &&
                browserFragment.isVisible() &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return;
        }
        super.onBackPressed();
    }

    private void initAmazonFactory() {
        if (!isAmazonFactoryInit) {
            factory = AmazonWebKitFactories.getDefaultFactory();
            if (factory.isRenderProcess(this)) {
                return; // Do nothing if this is on render process
            }
            factory.initialize(this.getApplicationContext());

            // factory configuration is done here, for example:
            factory.getCookieManager().setAcceptCookie(true);

            isAmazonFactoryInit = true;
        } else {
            factory = AmazonWebKitFactories.getDefaultFactory();
        }
    }

    @Override
    public void onNonTextInputUrlEntered(@NotNull final String urlStr) {
        ViewUtils.hideKeyboard(fragmentContainer);
        FragmentDispatcher.onUrlEnteredInner(urlStr, false, null, null,
                getSupportFragmentManager(), sessionManager, this);
    }

    @Override
    public void onTextInputUrlEntered(@NotNull final String urlStr,
            @NotNull final InlineAutocompleteEditText.AutocompleteResult autocompleteResult,
            @NotNull final UrlTextInputLocation inputLocation) {

        ViewUtils.hideKeyboard(fragmentContainer);
        // It'd be much cleaner/safer to do this with a kotlin callback.
        FragmentDispatcher.onUrlEnteredInner(urlStr, true, autocompleteResult, inputLocation,
                getSupportFragmentManager(), sessionManager, this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);

        if (browserFragment == null || !browserFragment.isVisible()) {
            return super.dispatchKeyEvent(event);
        }

        return browserFragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }
}
