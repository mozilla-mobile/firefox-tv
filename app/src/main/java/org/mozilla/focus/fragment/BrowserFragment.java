/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.architecture.NonNullObserver;
import org.mozilla.focus.ext.ContextKt;
import org.mozilla.focus.session.NullSession;
import org.mozilla.focus.session.Session;
import org.mozilla.focus.session.SessionCallbackProxy;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.Direction;
import org.mozilla.focus.utils.Edge;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.widget.AnimatedProgressBar;
import org.mozilla.focus.widget.Cursor;
import org.mozilla.focus.widget.CursorEvent;

/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends WebFragment implements CursorEvent {
    public static final String FRAGMENT_TAG = "browser";

    private static final String ARGUMENT_SESSION_UUID = "sessionUUID";
    private static final int SCROLL_MULTIPLIER = 45;

    public static BrowserFragment createForSession(Session session) {
        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_SESSION_UUID, session.getUUID());

        BrowserFragment fragment = new BrowserFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private Cursor cursor;

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private ViewGroup videoContainer;

    /**
     * Container containing the browser chrome and web content.
     */
    private View browserContainer;

    private IWebView.FullscreenCallback fullscreenCallback;

    private String url;
    private final SessionManager sessionManager;
    private Session session;

    public BrowserFragment() {
        sessionManager = SessionManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String sessionUUID = getArguments().getString(ARGUMENT_SESSION_UUID);
        if (sessionUUID == null) {
            throw new IllegalAccessError("No session exists");
        }

        session = sessionManager.hasSessionWithUUID(sessionUUID)
                ? sessionManager.getSessionByUUID(sessionUUID)
                : new NullSession();
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).updateHintNavigationVisibility(MainActivity.VideoPlayerState.BROWSER);
        }
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String getInitialUrl() {
        return session.getUrl().getValue();
    }

    @Override
    public View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browser, container, false);

        cursor = (Cursor) view.findViewById(R.id.cursor);
        cursor.cursorEvent = this;

        videoContainer = (ViewGroup) view.findViewById(R.id.video_container);
        browserContainer = view.findViewById(R.id.browser_container);

        session.getUrl().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String url) {
                BrowserFragment.this.url = url;
            }
        });

        setBlockingEnabled(session.isBlockingEnabled());

        session.getLoading().observe(this, new NonNullObserver<Boolean>() {
            @Override
            public void onValueChanged(@NonNull Boolean loading) {
                final MainActivity activity = (MainActivity)getActivity();
                updateCursorState();
                if (!loading && activity.isReloadingForYoutubeDrawerClosed) {
                    activity.isReloadingForYoutubeDrawerClosed = false;

                    // We send a play event which:
                    // - If we're on the video selection page, does nothing.
                    // - If we're in a fullscreen video, will show the play/pause controls on the screen so
                    // we don't just see a black screen.
                    activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                }
            }
        });

        return view;
    }

    @Override
    public IWebView.Callback createCallback() {
        return new SessionCallbackProxy(session, new IWebView.Callback() {
            @Override
            public void onPageStarted(final String url) {}

            @Override
            public void onPageFinished(boolean isSecure) {}

            @Override
            public void onURLChanged(final String url) {}

            @Override
            public void onRequest(boolean isTriggeredByUserGesture) {}

            @Override
            public void onProgress(int progress) {}

            @Override
            public void countBlockedTracker() {}

            @Override
            public void resetBlockedTrackers() {}

            @Override
            public void onBlockingStateChanged(boolean isBlockingEnabled) {}

            @Override
            public void onLongPress(final IWebView.HitTarget hitTarget) {}

            @Override
            public void onEnterFullScreen(@NonNull final IWebView.FullscreenCallback callback, @Nullable View view) {
                fullscreenCallback = callback;

                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer.setVisibility(View.INVISIBLE);

                    // Add view to video container and make it visible
                    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    videoContainer.addView(view, params);
                    videoContainer.setVisibility(View.VISIBLE);

                    // Switch to immersive mode: Hide system bars other UI controls
                    switchToImmersiveMode();
                }
            }

            @Override
            public void onExitFullScreen() {
                // Remove custom video views and hide container
                videoContainer.removeAllViews();
                videoContainer.setVisibility(View.GONE);

                // Show browser UI and web content again
                browserContainer.setVisibility(View.VISIBLE);

                exitImmersiveModeIfNeeded();

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback.fullScreenExited();
                    fullscreenCallback = null;
                }
            }
        });
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private void switchToImmersiveMode() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Show the system bars again.
     */
    private void exitImmersiveModeIfNeeded() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if ((WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON & activity.getWindow().getAttributes().flags) == 0) {
            // We left immersive mode already.
            return;
        }

        final Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // This fragment might get destroyed before the user left immersive mode (e.g. by opening another URL from an app).
        // In this case let's leave immersive mode now when the fragment gets destroyed.
        exitImmersiveModeIfNeeded();
    }

    public boolean onBackPressed() {
        if (canGoBack()) {
            // Go back in web history
            goBack();
            TelemetryWrapper.browserBackControllerEvent();
        } else {
            getFragmentManager().popBackStack();
            SessionManager.getInstance().removeCurrentSession();
        }

        return true;
    }

    @NonNull
    public String getUrl() {
        // getUrl() is used for things like sharing the current URL. We could try to use the webview,
        // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
        // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
        // user is probably expecting to share, so lets use that here:
        //
        // Note: when refactoring, I removed the url view and replaced urlView.setText with assignment
        // to this url variable - should be equivalent.
        return url;
    }

    public boolean canGoForward() {
        final IWebView webView = getWebView();
        return webView != null && webView.canGoForward();
    }

    public boolean canGoBack() {
        final IWebView webView = getWebView();
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.goBack();
        }
    }

    public void goForward() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.goForward();
        }
    }

    public void loadUrl(final String url) {
        final IWebView webView = getWebView();
        if (webView != null && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
    }

    public void reload() {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.reload();
        }
    }

    public void setBlockingEnabled(boolean enabled) {
        final IWebView webView = getWebView();
        if (webView != null) {
            webView.setBlockingEnabled(enabled);
        }
    }

    // --- TODO: CURSOR CODE - MODULARIZE IN #412. --- //
    /**
     * Gets the current state of the application and updates the cursor state accordingly.
     *
     * Note that this pattern could use some improvements:
     * - It's a little weird to get the current state from globals, rather than get passed in relevant values.
     * - BrowserFragment.setCursorEnabled should be called from this code path, but that's unclear
     * - BrowserFragment should use a listener to talk to MainActivity and shouldn't know about it directly.
     * - BrowserFragment calls MainActivity which calls BrowserFragment again - this is unnecessary.
     */
    public void updateCursorState() {
        final MainActivity activity = (MainActivity)getActivity();
        final IWebView webView = getWebView();
        // Bandaid null checks, underlying issue #249
        final boolean enableCursor = webView != null &&
                webView.getUrl() != null &&
                !webView.getUrl().contains("youtube.com/tv") &&
                getContext() != null &&
                !ContextKt.isVoiceViewEnabled(getContext()); // VoiceView has its own navigation controls.
        activity.setCursorEnabled(enableCursor);
    }

    public void moveCursor(Direction direction) {
        cursor.moveCursor(direction);
    }

    public void stopMoving(Direction direction) {
        cursor.stopMoving(direction);
    }

    public Point getCursorLocation() {
        return cursor.getLocation();
    }

    public void setCursorEnabled(boolean toEnable) {
        cursor.setVisibility(toEnable ? View.VISIBLE : View.GONE);
    }

    private int getScrollVelocity() {
        int speed = (int)cursor.getSpeed();
        return speed * SCROLL_MULTIPLIER;
    }

    public void cursorHitEdge(Edge edge) {
        IWebView webView = getWebView();
        if (webView == null) {
            return;
        }

        switch (edge) {
            case TOP:
                webView.flingScroll(0, -getScrollVelocity());
                break;
            case BOTTOM:
                webView.flingScroll(0, getScrollVelocity());
                break;
            case LEFT:
                webView.flingScroll(-getScrollVelocity(), 0);
                break;
            case RIGHT:
                webView.flingScroll(getScrollVelocity(), 0);
                break;
        }
    }
}
