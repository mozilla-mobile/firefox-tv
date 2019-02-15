/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.view.KeyEvent
import android.webkit.ValueCallback
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.ext.evalJS
import org.mozilla.tv.firefox.ext.handleYoutubeBack

/**
 * youtube/tv does not handle their back stack correctly. Going back in history visits redirects
 * which do not alter the UI. Users must navigate back by pressing ESC. To deal with this,
 * we remap BACK presses to ESC. When the top of YouTube's back stack is reached, we back
 * all the way out of YouTube.
 *
 * If YouTube guide-list element is focused:
 * - Suppress the DOWN,BACK key event
 * - Change the UP,BACK key event to go back in history before YouTube
 * Else:
 * - Dispatch ESC key event
 */

class YouTubeBackHandler(
    private val event: KeyEvent,
    private val engineView: EngineView?,
    private val activity: MainActivity
) {

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
        // This will return true if there is no focused element, the body tag is focused, or the
        // currently focused element is a button in the left sidebar. Otherwise it will return false
        engineView?.evalJS("""
                (function () {
                    return document.activeElement === null ||
                        document.activeElement.tagName === "BODY" ||
                        document.activeElement.parentElement.parentElement.id === 'guide-list';
                })();
                """,
                jsCallback)
    }

    private fun goBackBeforeYouTube() {
        engineView?.handleYoutubeBack()
    }
}
