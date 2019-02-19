/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.view.View

/**
 * When using spatial navigation (i.e. no Cursor) and the WebView loses and regains focus,
 * it fails to focus the previously focused DOMElement: this class provides functionality to
 * cache that focused DOMElement (in the DOM) and restore it. The chromium bug for WebView focus
 * is https://bugs.chromium.org/p/chromium/issues/detail?id=826577 Curiously, VoiceView doesn't
 * have this problem.
 *
 * youtube.com/tv does custom focus handling and keyboard navigation: when the WebView fails
 * to restore the focused element, dpad navigation is entirely broken (#393): that is
 * why this class is necessary.
 *
 * It is important that this value be cached just before the WebView regains focus.  Otherwise
 * edge cases present themselves where a DOM element is cached, a new element is focused by a
 * site, and then we return focus to the earlier (now incorrect) element (see #1830).  This
 * strategy alone does not cover all cases. Focus must also be cached (see #1850) when the
 * app is backgrounded, or pressing the home button or sleeping will lead to the wrong element
 * receiving focus when the app is resumed.
 *
 * NB: if you create an Android View which steals focus from the WebView and it refreshes the
 * DOM state (e.g. page reload), *you must add custom handling to that view* in order to cache
 * the focused DOMElement: on your view that returns (and maybe takes) focus to the WebView,
 * right before you call [View.setVisibility] (or overriding that method and right before calling
 * super), call [cache]. See [BrowserNavigationOverlay.setVisibility] for an example.
 * Unfortunately, there are no hooks to generically guarantee we cache the DOMElement the page
 * will finally set focus on so we require the view implementer to add cache the DOMElement at
 * the last possible moment before the WebView regains Android focus and loses the focused
 * DOMElement, which is in [View.setVisibility].
 */
interface FocusedDOMElementCacheInterface {
    /**
     * Caches the currently focused DOMElement in the DOM (i.e. it will be cleared if the page is reloaded).
     *
     * We cache focus whenever 1) the [WebRenderFragment] becomes the currently active screen or 2) the
     * WebView loses focus. Case 1 ensures that we always have the most up to date focus (see #1830). Case 2
     * ensures that we always have a cached value when the user backgrounds the app (see #1850).
     */
    fun cache()

    /** Focuses the cached DOMElement from [cache], if it's available, or does nothing. */
    fun restore()
}
