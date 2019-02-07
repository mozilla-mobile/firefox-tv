/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.ext.evalJS
import org.mozilla.tv.firefox.ext.toList
import org.mozilla.tv.firefox.utils.URLs

/**
 * youtube/tv does not handle their back stack correctly. Going back in history visits redirects which do not alter the UI.
 * Users must navigate back by pressing ESC. To deal with this, we remap BACK presses to ESC.
 * When the top of YouTube's back stack is reached, we back all the way out of YouTube.
 *
 * If YouTube guide-list element is focused:
 * - Suppress the DOWN,BACK key event
 * - Change the UP,BACK key event to go back in history before YouTube
 * Else:
 * - Dispatch ESC key event
 */

class YouTubeBackHandler(event: KeyEvent, engineView: EngineView?, activity: MainActivity) {
    private val event = event
    private val engineView = engineView
    private val activity = activity

    fun handleBackClick() {
        val jsCallback = ValueCallback<String> {
            if (it == "true") {
                if (event.action == KeyEvent.ACTION_DOWN) Unit
                else goBackBeforeYouTube()
            } else {
                val escKeyEvent = KeyEvent(event.action, KeyEvent.KEYCODE_ESCAPE)
                activity.dispatchKeyEvent(escKeyEvent)
            }
        }
        // This will return true if the currently focused YouTube element is a button in the left sidebar, false otherwise
        engineView?.evalJS("""
                (function () {
                    return document.activeElement.parentElement.parentElement.id === 'guide-list';
                })();
                """,
                jsCallback)
    }

    private fun goBackBeforeYouTube() {
        val webView = (engineView as ViewGroup).getChildAt(0) as WebView
        val backForwardUrlList = webView.copyBackForwardList().toList().map { it.originalUrl }
        val youtubeIndex = backForwardUrlList.lastIndexOf(URLs.YOUTUBE_TILE_URL)
        val goBackSteps = backForwardUrlList.size - youtubeIndex
        webView.goBackOrForward(-goBackSteps)
    }
}
