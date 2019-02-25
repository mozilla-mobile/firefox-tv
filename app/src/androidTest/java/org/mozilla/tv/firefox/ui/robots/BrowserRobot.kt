/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import androidx.test.InstrumentationRegistry
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.equalTo

class BrowserRobot {

    /**
     * Executes the given JS string.
     * @return if the final expression is a return statement, returns the provided String, else null.
     */
    fun executeJS(js: String): String? {
        return webView()
            .perform(Atoms.script(js))
            .get().value as? String
    }

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
