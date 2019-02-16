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

// This will only happen if YouTube is loading or navigation has broken
private const val noElementFocused = "document.activeElement === null"
// This will only happen if YouTube is loading or navigation has broken
private const val bodyElementFocused = "document.activeElement.tagName === \"BODY\""
private const val sidebarFocused = "document.activeElement.parentElement.parentElement.id === 'guide-list'"

/**
 * youtube/tv does not handle their back stack correctly. Going back in history visits redirects
 * which do not alter the UI. Users must navigate back by pressing ESC. To deal with this,
 * we remap BACK presses to ESC. When the top of YouTube's back stack is reached, or we are in
 * an unexpected state, we back all the way out of YouTube.
 *
 * If YouTube guide-list element is focused or we are in an unexpected state:
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

        val shouldWeExitPage = """
               (function () {
                    return $noElementFocused ||
                        $bodyElementFocused ||
                        $sidebarFocused;
                })();
        """.trimIndent()

        val backOrMoveFocus = ValueCallback<String> { shouldExitPage ->
            if (shouldExitPage == "true") {
                if (event.action == KeyEvent.ACTION_DOWN) Unit
                else goBackBeforeYouTube()
            } else {
                // Esc will move focus within YouTube
                val escKeyEvent = KeyEvent(event.action, KeyEvent.KEYCODE_ESCAPE)
                activity.dispatchKeyEvent(escKeyEvent)
            }
        }

        engineView?.evalJS(javascript = shouldWeExitPage, callback = backOrMoveFocus)
    }

    private fun goBackBeforeYouTube() {
        engineView?.handleYoutubeBack()
    }
}
