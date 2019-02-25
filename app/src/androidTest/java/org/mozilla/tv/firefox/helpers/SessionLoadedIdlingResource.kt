/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingResource
import org.mozilla.tv.firefox.FirefoxApplication

/**
 * An IdlingResource implementation that waits until the current session is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
class SessionLoadedIdlingResource : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    var waitUntil: (() -> Boolean)? = null

    override fun getName(): String {
        return SessionLoadedIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        val context = ApplicationProvider.getApplicationContext<FirefoxApplication>()
        val sessionManager = context.components.sessionManager

        val session = sessionManager.selectedSession

        /**
         * Used for [org.mozilla.tv.firefox.ui.YouTubeNavigationTest].
         * This will cause the test to wait until a specific condition is true before moving on.
         */
        val waitUntil = waitUntil

        return if (session?.loading == true) {
            false
        } else {
            when {
                waitUntil == null -> {}
                waitUntil() -> this.waitUntil = null
                !waitUntil() -> return false
            }
            invokeCallback()
            true
        }
    }

    private fun invokeCallback() {
        if (resourceCallback != null) {
            resourceCallback!!.onTransitionToIdle()
        }
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.resourceCallback = callback
    }
}
