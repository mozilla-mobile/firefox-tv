/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.amazon.android.webkit.AmazonWebKitFactories;
import com.amazon.android.webkit.AmazonWebKitFactory;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.R;
import org.mozilla.focus.architecture.NonNullObserver;
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.HomeFragment;
import org.mozilla.focus.fragment.NewSettingsFragment;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.session.Session;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.session.Source;
import org.mozilla.focus.telemetry.MenuAppNavButton;
import org.mozilla.focus.telemetry.MenuBrowserNavButton;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.telemetry.UrlTextInputLocation;
import org.mozilla.focus.utils.OnUrlEnteredListener;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ThreadUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.InlineAutocompleteEditText;

import java.util.List;


public class MainActivity extends LocaleAwareAppCompatActivity implements OnUrlEnteredListener, View.OnClickListener {

    public static final String ACTION_ERASE = "erase";
    public static final String ACTION_OPEN = "open";

    public static final String EXTRA_TEXT_SELECTION = "text_selection";
    public static final String EXTRA_NOTIFICATION = "notification";

    private static final String EXTRA_SHORTCUT = "shortcut";

    private static final int ONBOARDING_REQ_CODE = 1;

    private final SessionManager sessionManager;
    private final UrlAutoCompleteFilter drawerUrlAutoCompleteFilter = new UrlAutoCompleteFilter();

    private DrawerLayout drawer;
    private NavigationView fragmentNavigationBar;
    private View fragmentContainer;
    private InlineAutocompleteEditText drawerUrlInput;

    private ImageButton drawerRefresh;
    private ImageButton drawerForward;
    private ImageButton drawerBack;
    private Switch drawerTrackingProtectionSwitch;
    private LinearLayout customNavItem;
    private boolean isDrawerOpen = false;

    public enum VideoPlayerState {
       BROWSER, HOME, SETTINGS
    }

    public MainActivity() {
        sessionManager = SessionManager.getInstance();
    }

    // We need to respond to the onPageFinished event so we set a flag here.
    public boolean isReloadingForYoutubeDrawerClosed = false;

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
        drawer = findViewById(R.id.drawer_layout);
        drawerRefresh = findViewById(R.id.drawer_refresh_button);
        drawerRefresh.setOnClickListener(this);
        drawerForward = findViewById(R.id.drawer_forward_button);
        drawerForward.setOnClickListener(this);
        drawerBack = findViewById(R.id.drawer_back_button);
        drawerBack.setOnClickListener(this);
        drawerTrackingProtectionSwitch = findViewById(R.id.tracking_protection_switch);

        // setChecked must be called before we add the listener, otherwise the listener will fired when we
        // call setChecked this first time. In particular, this would make telemetry inaccurate.
        final boolean trackingProtectionEnabled = Settings.getInstance(this).isBlockingEnabled();
        drawerTrackingProtectionSwitch.setChecked(trackingProtectionEnabled);
        setTrackingProtectionSwitchOn(trackingProtectionEnabled);
        drawerTrackingProtectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.getInstance(MainActivity.this).setBlockingEnabled(b);
                setTrackingProtectionSwitchOn(b);
                TelemetryWrapper.turboModeSwitchEvent(b);

                ThreadUtils.postToMainThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final BrowserFragment browserFragment = (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
                        if (browserFragment != null) {
                            browserFragment.reload();
                        }
                        drawer.closeDrawer(GravityCompat.START);
                    }
                }, /* Switch.THUMB_ANIMATION_DURATION */ 250);
            }
        });

        // todo: remove amiguity between navigation bars.
        fragmentNavigationBar = findViewById(R.id.fragment_navigation);
        fragmentNavigationBar.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
                final MenuAppNavButton button;
                switch (item.getItemId()) {
                    case R.id.drawer_home:
                        button = MenuAppNavButton.HOME;
                        showHomeScreen();
                        break;

                    case R.id.drawer_settings:
                        button = MenuAppNavButton.SETTINGS;
                        showSettingsScreen();
                        break;

                    default:
                        return false;
                }

                TelemetryWrapper.menuAppNavEvent(button);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(final View drawerView) {
                findViewById(R.id.urlView).requestFocus();
                isDrawerOpen = true;

                // Stop cursor movement upon drawer opening
                // Need to fix follow-up issue https://github.com/mozilla-mobile/focus-video/issues/219
                final BrowserFragment browserFragment = (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
                if (browserFragment != null &&
                        browserFragment.getCursor() != null) {
                    // For all intents and purposes, covering the UI with a menu is the same as onPause.
                    browserFragment.getCursor().onPause();
                }

                TelemetryWrapper.drawerShowHideEvent(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                isDrawerOpen = false;

                // If you open and close the menu on youtube, dpad navigation stops working for an unknown reason.
                // One workaround is to reload the page, which we do here.
                final BrowserFragment browserFragment = (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
                if (browserFragment != null && browserFragment.isVisible() && browserFragment.getUrl().contains("youtube.com/tv")) {
                    browserFragment.reload();
                    isReloadingForYoutubeDrawerClosed = true;
                }

                if (browserFragment != null && browserFragment.getCursor() != null) {
                    // For all intents and purposes, covering the UI with a menu is the same as onPause.
                    browserFragment.getCursor().onResume();
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                updateDrawerNavUI();
            }
        });

        drawerUrlInput = findViewById(R.id.urlView);
        drawerUrlInput.setImeOptions(drawerUrlInput.getImeOptions() | ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING);
        drawerUrlInput.setOnCommitListener(new InlineAutocompleteEditText.OnCommitListener() {
            @Override
            public void onCommit() {
                final String userInput = drawerUrlInput.getText().toString();
                if (!TextUtils.isEmpty(userInput)) {
                    // getLastAutocompleteResult must be called before closeDrawer: closeDrawer clears the text input,
                    // which clears the last autocomplete result.
                    onTextInputUrlEntered(userInput, drawerUrlInput.getLastAutocompleteResult(), UrlTextInputLocation.MENU);
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });
        drawerUrlInput.setOnFilterListener(new InlineAutocompleteEditText.OnFilterListener() {
            @Override
            public void onFilter(final String searchText, final InlineAutocompleteEditText view) {
                drawerUrlAutoCompleteFilter.onFilter(searchText, view);
            }
        });

        drawerUrlInput.setOnBackPressedListener(new InlineAutocompleteEditText.OnBackPressedListener() {
            @Override
            public void onBackPressed() {
                if (isDrawerOpen) {
                    drawer.requestFocus();
                    drawerUrlInput.requestFocus();
                }
            }
        });

        final SafeIntent intent = new SafeIntent(getIntent());

        if (intent.isLauncherIntent()) {
            TelemetryWrapper.openFromIconEvent();
        }

        customNavItem = findViewById(R.id.custom_button_layout);

        sessionManager.handleIntent(this, intent, savedInstanceState);

        sessionManager.getSessions().observe(this,  new NonNullObserver<List<Session>>() {
            @Override
            public void onValueChanged(@NonNull List<Session> sessions) {
                if (sessions.isEmpty()) {
                    // There's no active session. Show the URL input screen so that the user can
                    // start a new session.
                    showHomeScreen();
                } else {
                    showBrowserScreenForCurrentSession();
                }

                if (Settings.getInstance(MainActivity.this).shouldShowOnboarding()) {
                    showOnboardingScreen();
                }
            }
        });

        WebViewProvider.preload(this);
    }

    private void setTrackingProtectionSwitchOn(boolean isEnabled) {
        if (isEnabled) {
            drawerTrackingProtectionSwitch.setAlpha(1f);
        } else {
            drawerTrackingProtectionSwitch.setAlpha(.6f);
        }
    }

    @Override
    public void onClick(View view) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment fragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (fragment == null || !fragment.isVisible()) {
            return;
        }

        final MenuBrowserNavButton navButton;
        switch (view.getId()) {
            case R.id.drawer_refresh_button:
                navButton = MenuBrowserNavButton.REFRESH;
                fragment.reload();
                break;
            case R.id.drawer_back_button:
                navButton = MenuBrowserNavButton.BACK;
                if (fragment.canGoBack()) {
                    fragment.goBack();
                }
                break;
            case R.id.drawer_forward_button:
                navButton = MenuBrowserNavButton.FORWARD;
                if (fragment.canGoForward()) {
                    fragment.goForward();
                }
                break;
            default:
                // Return so that we don't try to close the drawer
                return;
        }

        TelemetryWrapper.menuBrowserNavEvent(navButton);
        drawer.closeDrawer(GravityCompat.START);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Duplicates logic in toggleDrawer but since we only want to record telemetry for user input,
            // we can't put this inside toggleDrawer, which could be used internally.
            //
            // We record the drawer open telemetry elsewhere.
            if (isDrawerOpen) {
                TelemetryWrapper.drawerShowHideEvent(false);
            }
            toggleDrawer();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void toggleDrawer() {
        updateDrawerNavUI();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            drawer.openDrawer(GravityCompat.START);
        }
    }
    private void updateDrawerNavUI() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (customNavItem.getVisibility() == View.VISIBLE && browserFragment != null && browserFragment.isVisible()) {
            drawerForward.setFocusable(browserFragment.canGoForward());
            drawerForward.setColorFilter((browserFragment.canGoForward() ? Color.WHITE : ContextCompat.getColor(this, R.color.colorTextInactive)), android.graphics.PorterDuff.Mode.SRC_IN);
            drawerBack.setColorFilter((browserFragment.canGoBack() ? Color.WHITE : ContextCompat.getColor(this, R.color.colorTextInactive)), android.graphics.PorterDuff.Mode.SRC_IN);
            drawerBack.setFocusable(browserFragment.canGoBack());
            drawerUrlInput.setText(browserFragment.getUrl());
            drawerRefresh.setFocusable(true);
            drawerRefresh.setColorFilter(Color.WHITE);
        } else {
            drawerForward.setFocusable(false);
            drawerForward.setColorFilter(ContextCompat.getColor(this, R.color.colorTextInactive), android.graphics.PorterDuff.Mode.SRC_IN);
            drawerBack.setColorFilter(ContextCompat.getColor(this, R.color.colorTextInactive), android.graphics.PorterDuff.Mode.SRC_IN);
            drawerBack.setFocusable(false);
            drawerRefresh.setFocusable(false);
            drawerRefresh.setColorFilter(ContextCompat.getColor(this, R.color.colorTextInactive), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        final HomeFragment homeFragment = (HomeFragment) fragmentManager.findFragmentByTag (HomeFragment.FRAGMENT_TAG);
        if (homeFragment != null && homeFragment.isVisible()) {
            drawerUrlInput.setText("");
        }
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

        drawerUrlAutoCompleteFilter.load(getApplicationContext(), /* using kotlin default value */ true);
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

    public void showHomeScreen() {
        // TODO: animations if fragment is found.
        final HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        if (homeFragment != null && homeFragment.isVisible()) {
            // This is already at the top of the stack - do nothing.
            return;
        }

        // We don't want to be able to go back from the back stack, so clear the whole fragment back stack.
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Show the home screen.
        final HomeFragment newHomeFragment = HomeFragment.create();
        newHomeFragment.setOnUrlEnteredListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, newHomeFragment, HomeFragment.FRAGMENT_TAG)
                .commit();
    }

    public void showSettingsScreen() {
        // TODO: animations if fragment is found.
        final NewSettingsFragment settingsFragment = NewSettingsFragment.create();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, settingsFragment, NewSettingsFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    private void showOnboardingScreen() {
        final Intent intent = new Intent(this, OnboardingActivity.class);
        startActivityForResult(intent, ONBOARDING_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ONBOARDING_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                // We don't need to check the result because it is only RESULT_OK in one case.
                drawerTrackingProtectionSwitch.setChecked(false);
            }
        }
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
                .addToBackStack(null)
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

        if (isDrawerOpen) {
            drawer.closeDrawer(GravityCompat.START);
            TelemetryWrapper.drawerShowHideEvent(false);
            return;
        }

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
        onUrlEnteredInner(urlStr, false, null, null);
    }

    @Override
    public void onTextInputUrlEntered(@NotNull final String urlStr,
            @NotNull final InlineAutocompleteEditText.AutocompleteResult autocompleteResult,
            @NotNull final UrlTextInputLocation inputLocation) {
        // It'd be much cleaner/safer to do this with a kotlin callback.
        onUrlEnteredInner(urlStr, true, autocompleteResult, inputLocation);
    }

    // todo: naming
    // todo: to make MainActivity smaller, this should move to a single responsibility class like FragmentDispatcher
    /**
     * Loads the given url. If isTextInput is true, there should be no null parameters.
     */
    private void onUrlEnteredInner(final String urlStr, final boolean isTextInput,
            @Nullable final InlineAutocompleteEditText.AutocompleteResult autocompleteResult,
            @Nullable final UrlTextInputLocation inputLocation) {
        if (TextUtils.isEmpty(urlStr.trim())) {
            return;
        }

        ViewUtils.hideKeyboard(fragmentContainer);

        final boolean isUrl = UrlUtils.isUrl(urlStr);
        final String updatedUrlStr;
        final String searchTerms;
        if (isUrl) {
            updatedUrlStr = UrlUtils.normalize(urlStr);
            searchTerms = null;
        } else {
            updatedUrlStr = UrlUtils.createSearchUrl(this, urlStr);
            searchTerms = urlStr.trim();
        }

        if (sessionManager.hasSession()) {
            sessionManager.getCurrentSession().setSearchTerms(searchTerms); // todo: correct?
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();

        // TODO: could this ever happen where browserFragment is on top? and do we need to do anything special for it?
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        final boolean isSearch = !TextUtils.isEmpty(searchTerms);
        if (browserFragment != null && browserFragment.isVisible()) {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFragment.loadUrl(updatedUrlStr);

            // And this fragment can be removed again.
            fragmentManager.beginTransaction()
                    .replace(R.id.container, browserFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            if (isSearch) {
                SessionManager.getInstance().createSearchSession(Source.USER_ENTERED, updatedUrlStr, searchTerms);
            } else {
                SessionManager.getInstance().createSession(Source.USER_ENTERED, updatedUrlStr);
            }
        }

        if (isTextInput) {
            // Non-text input events are handled at the source, e.g. home tile click events.
            if (autocompleteResult == null) {
                throw new IllegalArgumentException("Expected non-null autocomplete result for text input");
            }
            if (inputLocation == null) {
                throw new IllegalArgumentException("Expected non-null input location for text input");
            }

            TelemetryWrapper.urlBarEvent(!isSearch, autocompleteResult, inputLocation);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);

        // Remaps the back button to escape when:
        // - The browser is visible
        // - We're on youtube (i.e. the cursor is disabled).
        // - The drawer isn't open
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && browserFragment != null && browserFragment.isVisible() &&
                browserFragment.getUrl().contains("youtube.com/tv") && !isDrawerOpen) {
            KeyEvent keyEvent = new KeyEvent(event.getAction(), KeyEvent.KEYCODE_ESCAPE);
            dispatchKeyEvent(keyEvent);
            return true;
        }

        if (browserFragment == null || !browserFragment.isVisible() || isDrawerOpen) {
            return super.dispatchKeyEvent(event);
        }

        return browserFragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }
}
