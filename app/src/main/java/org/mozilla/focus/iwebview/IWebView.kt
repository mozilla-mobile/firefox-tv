/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.iwebview

import android.graphics.Bitmap
import android.support.annotation.CheckResult
import android.view.View
import org.mozilla.focus.session.Session

interface IWebView {

    companion object {
        const val TRACKING_PROTECTION_ENABLED_PREF = "tracking_protection_enabled"
        const val TRACKING_PROTECTION_ENABLED_DEFAULT = true
    }

    /** Get the title of the currently displayed website. */
    fun getTitle(): String? // nullable because WebView overrides it. #407
    fun getUrl(): String? // nullable because WebView overrides it. #407

    var callback: Callback?

    fun onStart()
    fun onStop()
    fun pauseTimers()
    fun resumeTimers()

    fun loadUrl(url: String)
    fun evalJS(js: String)
    fun stopLoading()
    fun reload()

    fun goForward()
    fun goBack()

    fun canGoForward(): Boolean
    fun canGoBack(): Boolean

    fun addJavascriptInterface(obj: Any?, interfaceName: String?)
    fun removeJavascriptInterface(interfaceName: String?)

    /**
     * Scrolls the page but clamps it to the page dimensions, unlike [View.scrollBy].
     *
     * #712: View.scrollBy will "overscroll" the page dimensions, e.g. for a page of length 500,
     * scrollBy can increment scrollY to greater values like 1000. After this, calling
     * scrollBy upwards to scrollY = 750 will still not display any changes to the user: it has
     * to make up that lost distance first.
     *
     * Scrolling and clamping appears to be the only way to prevent overscroll in Kotlin/Java,
     * which does not return the content size: https://stackoverflow.com/a/49439945/2219998. You
     * could also implement scrolling in JavaScript, but there's presumably a performance cost to
     * executing JS, separating the code could make it hard to modify, and the code would need to
     * be aware of which DOM element is being scrolled.
     *
     * For AmazonWebView, I verified that a page whose content shrinks when the user is scrolled all
     * the way to the bottom will not create an upwards scroll dead zone.
     */
    fun scrollByClamped(vx: Int, vy: Int)
    fun requestFocus(): Boolean

    fun cleanup()

    fun restoreWebViewState(session: Session)
    fun saveWebViewState(session: Session)
    fun destroy()

    /**
     * Take a screenshot of the screen.
     * @return a bitmap with the contents of the screen: *don't forget to recycle it!*
     */
    @CheckResult
    fun takeScreenshot(): Bitmap

    val isYoutubeTV: Boolean
        get() {
            return getUrl()?.contains("youtube.com/tv") ?: false
        }

    val focusedDOMElement: FocusedDOMElementCache

    /**
     * Enable/Disable content blocking for this session (Only the blockers that are
     * enabled in the app's settings will be turned on/off).
     */
    fun setBlockingEnabled(enabled: Boolean)

    class HitTarget(isLink: Boolean, linkURL: String?, isImage: Boolean, imageURL: String?) {
        init {
            if (isLink && linkURL == null) {
                throw IllegalStateException("link hittarget must contain URL")
            } else if (isImage && imageURL == null) {
                throw IllegalStateException("image hittarget must contain URL")
            }
        }
    }

    interface Callback {
        fun onPageStarted(url: String)
        fun onPageFinished(isSecure: Boolean)
        fun onProgress(progress: Int)

        fun onURLChanged(url: String)

        fun onRequest(isTriggeredByUserGesture: Boolean)

        fun onLongPress(hitTarget: HitTarget)

        /**
         * Notify the host application that the current page has entered full screen mode.
         *
         * The callback needs to be invoked to request the page to exit full screen mode.
         *
         * Some IWebView implementations may pass a custom View which contains the web contents in
         * full screen mode.
         */
        fun onEnterFullScreen(callback: FullscreenCallback, view: View?)

        /**
         * Notify the host application that the current page has exited full screen mode.
         *
         * If a View was passed when the application entered full screen mode then this view must
         * be hidden now.
         */
        fun onExitFullScreen()

        fun onBlockingStateChanged(isBlockingEnabled: Boolean)
    }

    interface FullscreenCallback {
        fun fullScreenExited()
    }
}
