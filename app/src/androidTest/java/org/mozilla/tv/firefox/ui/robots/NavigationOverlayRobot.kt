/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.clearText
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers.hasDescendant
import android.support.test.espresso.matcher.ViewMatchers.withHint
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.Until
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertTrue
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.RecyclerViewHelpers
import org.mozilla.tv.firefox.helpers.ext.assertIsChecked
import org.mozilla.tv.firefox.helpers.ext.assertIsEnabled
import org.mozilla.tv.firefox.helpers.ext.assertIsSelected
import org.mozilla.tv.firefox.helpers.ext.click
import org.mozilla.tv.firefox.pinnedtile.TileViewHolder

/**
 * Implementation of Robot Pattern for the navigation overlay menu.
 */
class NavigationOverlayRobot {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun goBack() = backButton().click()
    fun remoteBack() = device.pressBack()
    fun goForward() = forwardButton().click()
    fun reload() = reloadButton().click()
    fun toggleTurbo() = turboButton().click()
    fun openSettings() = settingsButton().click()

    fun assertCanGoBack(canGoBack: Boolean) = backButton().assertIsEnabled(canGoBack)
    fun assertCanGoForward(canGoForward: Boolean) = forwardButton().assertIsEnabled(canGoForward)
    fun assertCanGoBackForward(canGoBack: Boolean, canGoForward: Boolean) {
        assertCanGoBack(canGoBack)
        assertCanGoForward(canGoForward)
    }

    fun assertCanReload(canReload: Boolean) = reloadButton().assertIsEnabled(canReload)
    fun assertTurboIsSelected(isEnabled: Boolean) = turboButton().assertIsSelected(isEnabled)

    fun assertPinButtonChecked(checked: Boolean) = innerAssertPinButtonChecked(checked)

    fun assertURLBarTextContains(expectedText: String) = urlBar().check(matches(withText(containsString(expectedText))))

    fun assertURLBarDisplaysHint() {
        assertURLBarTextContains("")
                .check(matches(withHint(R.string.urlbar_hint)))
    }

    fun assertDesktopModeEnabled(desktopModeEnabled: Boolean) = desktopModeButton().assertIsEnabled(desktopModeEnabled)

    fun assertPinnedTileExists(inPosition: Int, withText: String) = {
        device.wait(Until.findObject(By.textContains(withText)), 200)
        homeTiles()
        .check(matches(RecyclerViewHelpers.atPosition(inPosition, hasDescendant(withText(withText)))))
    }

    fun assertActivityFinishing(activity: MainActivityTestRule) = assertTrue(activity.activity.isFinishing)

    class Transition {
        private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun enterUrlAndEnterToBrowser(url: Uri, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            urlBar().perform(clearText(),
                    typeText(url.toString()),
                    pressImeActionButton())

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun closeToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            device.pressMenu()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openPocketMegatile(interact: PocketRecommendedVideosRobot.() -> Unit): PocketRecommendedVideosRobot.Transition {
            pocketMegaTile().click()

            PocketRecommendedVideosRobot().interact()
            return PocketRecommendedVideosRobot.Transition()
        }

        fun openTileToBrowser(index: Int, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            homeTiles().perform(RecyclerViewActions.actionOnItemAtPosition<TileViewHolder>(index, click()))

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openTileToBrowser(title: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            homeTiles().perform(RecyclerViewActions.actionOnItem<TileViewHolder>(hasDescendant(withText(title)), click()))

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openSettings(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            settingsButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        private fun assertCanTurnDesktopModeOn(canTurnDesktopModeOn: Boolean) = desktopModeButton().assertIsChecked(!canTurnDesktopModeOn)

        fun turnDesktopModeOn(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            assertCanTurnDesktopModeOn(true)
            desktopModeButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun turnDesktopModeOff(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            assertCanTurnDesktopModeOn(false)
            desktopModeButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goBack(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            backButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goForward(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            forwardButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun reload(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            reloadButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun toggleTurbo(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            turboButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun pinSite(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            innerAssertPinButtonChecked(false)
            pinButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun unpinSite(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            innerAssertPinButtonChecked(true)
            pinButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

/**
 * Applies [interact] to a new [NavigationOverlayRobot]
 *
 * @sample org.mozilla.tv.firefox.session.ClearSessionTest.WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable
 */
fun navigationOverlay(interact: NavigationOverlayRobot.() -> Unit): NavigationOverlayRobot.Transition {
    NavigationOverlayRobot().interact()
    return NavigationOverlayRobot.Transition()
}

private fun innerAssertPinButtonChecked(checked: Boolean) = pinButton().assertIsChecked(checked)

private fun backButton() = onView(withId(R.id.navButtonBack))
private fun forwardButton() = onView(withId(R.id.navButtonForward))
private fun reloadButton() = onView(withId(R.id.navButtonReload))
private fun pinButton() = onView(withId(R.id.pinButton))
private fun turboButton() = onView(withId(R.id.turboButton))
private fun settingsButton() = onView(withId(R.id.navButtonSettings))
private fun urlBar() = onView(withId(R.id.navUrlInput))
private fun homeTiles() = onView(withId(R.id.tileContainer))
private fun overlay() = onView(withId(R.layout.browser_overlay))
private fun desktopModeButton() = onView(withId(R.id.desktopModeButton))
private fun pocketMegaTile() = onView(withId(R.id.pocketVideosContainer))
