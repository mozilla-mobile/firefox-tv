/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
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
import org.mozilla.focus.fragment.HomeFragment;
import org.mozilla.focus.fragment.OnUrlEnteredListener;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.session.Session;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.session.Source;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;

import java.util.List;

public class MainActivity extends LocaleAwareAppCompatActivity implements OnUrlEnteredListener {
    public static final String ACTION_ERASE = "erase";
    public static final String ACTION_OPEN = "open";

    public static final String EXTRA_TEXT_SELECTION = "text_selection";
    public static final String EXTRA_NOTIFICATION = "notification";

    private static final String EXTRA_SHORTCUT = "shortcut";

    private final SessionManager sessionManager;

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

        final SafeIntent intent = new SafeIntent(getIntent());

        if (intent.isLauncherIntent()) {
            TelemetryWrapper.openFromIconEvent();
        }

        sessionManager.handleIntent(this, intent, savedInstanceState);

        sessionManager.getSessions().observe(this,  new NonNullObserver<List<Session>>() {
            private boolean wasSessionsEmpty = false;

            @Override
            public void onValueChanged(@NonNull List<Session> sessions) {
                if (sessions.isEmpty()) {
                    // There's no active session. Show the URL input screen so that the user can
                    // start a new session.
                    showHomeScreen();
                    wasSessionsEmpty = true;
                } else {
                    // This happens when we move from 0 to 1 sessions: either on startup or after an erase.
                    if (wasSessionsEmpty) {
                        WebViewProvider.performNewBrowserSessionCleanup();
                        wasSessionsEmpty = false;
                    }

                    // We have at least one session. Show a fragment for the current session.
                    showBrowserScreenForCurrentSession();
                }
            }
        });

        WebViewProvider.preload(this);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            final DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        if (isFinishing()) {
            WebViewProvider.performCleanup(this);
        }

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

        final String action = intent.getAction();

        if (ACTION_OPEN.equals(action)) {
            TelemetryWrapper.openNotificationActionEvent();
        }

        if (ACTION_ERASE.equals(action)) {
            processEraseAction(intent);
        }

        if (intent.isLauncherIntent()) {
            TelemetryWrapper.resumeFromIconEvent();
        }
    }

    private void processEraseAction(final SafeIntent intent) {
        final boolean fromShortcut = intent.getBooleanExtra(EXTRA_SHORTCUT, false);
        final boolean fromNotification = intent.getBooleanExtra(EXTRA_NOTIFICATION, false);

        SessionManager.getInstance().removeAllSessions();

        if (fromShortcut) {
            TelemetryWrapper.eraseShortcutEvent();
        } else if (fromNotification) {
            TelemetryWrapper.eraseAndOpenNotificationActionEvent();
        }
    }

    private void showHomeScreen() {
        // TODO: animations if fragment is found.
        final HomeFragment homeFragment = HomeFragment.create();
        homeFragment.setOnUrlEnteredListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, homeFragment, HomeFragment.FRAGMENT_TAG)
                .commit();
    }

    private void showBrowserScreenForCurrentSession() {
        final Session currentSession = sessionManager.getCurrentSession();
        final FragmentManager fragmentManager = getSupportFragmentManager();

        final BrowserFragment fragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (fragment != null && fragment.getSession().isSameAs(currentSession)) {
            // There's already a BrowserFragment displaying this session.
            return;
        }

        fragmentManager
                .beginTransaction()
                .replace(R.id.container,
                        BrowserFragment.createForSession(currentSession), BrowserFragment.FRAGMENT_TAG)
                .commit();
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

        // todo: homefragment?

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

    // todo: naming
    // todo: to make MainActivity smaller, this should move to a single responsibility class like FragmentDispatcher
    @Override
    public void onUrlEntered(@NotNull final String urlStr, @Nullable final String searchTerms) {
        if (sessionManager.hasSession()) sessionManager.getCurrentSession().setSearchTerms(searchTerms); // todo: correct?

        final FragmentManager fragmentManager = getSupportFragmentManager();

        // TODO: could this ever happen where browserFragment is on top? and do we need to do anything special for it?
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (browserFragment != null && browserFragment.isVisible()) {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFragment.loadUrl(urlStr);

            // And this fragment can be removed again.
            fragmentManager.beginTransaction()
                    .replace(R.id.container, browserFragment)
                    .commit();
        } else {
            if (!TextUtils.isEmpty(searchTerms)) {
                SessionManager.getInstance().createSearchSession(Source.USER_ENTERED, urlStr, searchTerms);
            } else {
                SessionManager.getInstance().createSession(Source.USER_ENTERED, urlStr);
            }
        }
    }
}
