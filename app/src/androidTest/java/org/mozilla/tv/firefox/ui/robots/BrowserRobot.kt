/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.webkit.CookieManager
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull

const val TEST_COOKIE_URL = "TEST_COOKIE_KEY"
const val TEST_COOKIE_VALUE = "TEST_COOKIE_VALUE"

class BrowserRobot {

    private val cookieManager = CookieManager.getInstance()

    fun addTestCookie() {
        cookieManager.setCookie(TEST_COOKIE_URL, TEST_COOKIE_VALUE)
    }

    fun assertCookieExists() {
        assertNotNull(cookieManager.getCookie(TEST_COOKIE_URL))
    }

    fun assertCookieDoesNotExist() {
        assertNull(cookieManager.getCookie(TEST_COOKIE_URL))
    }
}

fun browser(func: BrowserRobot.() -> Unit) = BrowserRobot().func()
