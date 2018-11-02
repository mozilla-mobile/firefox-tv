/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.model.Atoms
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import android.support.test.espresso.web.webdriver.Locator
import android.support.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.equalTo

class BrowserRobot {

    fun getPageTitle(): String {
        return webView()
                .perform(Atoms.getTitle())
                .get()
    }

    /**
     * Asserts that the text within DOM element with ID="testContent" has the given text, i.e.
     *   document.querySelector('#testContent').innerText == expectedText
     */
    fun assertTestContent(expectedText: String) {
        webView().withElement(findElement(Locator.ID, "testContent"))
                .check(webMatches(getText(), equalTo(expectedText)))
    }

    fun assertDOMElementExists(locator: Locator, value: String) {
        webView().withElement(findElement(locator, value))
    }

    class Transition {
        private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
            device.pressMenu()

            NavigationOverlayRobot().interact()
            return NavigationOverlayRobot.Transition()
        }
    }
}

fun browser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
    BrowserRobot().interact()
    return BrowserRobot.Transition()
}

private fun webView() = onWebView()
