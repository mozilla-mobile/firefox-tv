/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.webkit.CookieManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

const val TEST_COOKIE_URL = "TEST_COOKIE_KEY"
const val TEST_COOKIE_VALUE = "TEST_COOKIE_VALUE"

/**
 * A robot to modify the internals of the engine.
 */
class EngineInternalsRobot {
    private val cookieManager = CookieManager.getInstance()
    private var testCookie: String?
        get() = cookieManager.getCookie(TEST_COOKIE_URL)
        set(value) { cookieManager.setCookie(TEST_COOKIE_URL, value) }

    fun addCookie() {
        testCookie = TEST_COOKIE_VALUE
    }

    fun assertCookieExists() {
        assertNotNull(testCookie)
    }

    fun assertCookieDoesNotExist() {
        assertNull(testCookie)
    }
}

fun engineInternals(interact: EngineInternalsRobot.() -> Unit) {
    EngineInternalsRobot().interact()
}
