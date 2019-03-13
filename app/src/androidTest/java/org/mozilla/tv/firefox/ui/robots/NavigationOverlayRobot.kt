/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertTrue
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.RecyclerViewHelpers
import org.mozilla.tv.firefox.helpers.ext.assertIsChecked
import org.mozilla.tv.firefox.helpers.ext.assertIsDisplayed
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

    // The implementation of this method is arbitrary. We could run this check
    // against any of its views
    fun assertOverlayIsOpen() = urlBar().assertIsDisplayed()

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

    fun waitForURLBarToDisplayHint() {
        device.wait(Until.findObject(By.res(R.id.navUrlInput.toString()).textContains("")), 5000)
    }

    fun assertDesktopModeEnabled(desktopModeEnabled: Boolean) = desktopModeButton().assertIsEnabled(desktopModeEnabled)

    fun assertPinnedTileExists(inPosition: Int, withText: String) {
        // Tile takes a short amount of time to pop up because we have a coroutine that loads all parts of
        // the tile at the same time. Checking Idling doesn't work (and it just waits up to 10s for idle
        // anyway.
        device.wait(Until.findObject(By.textContains(withText)), 2000)
        homeTiles().check(matches(RecyclerViewHelpers
                .atPosition(inPosition, hasDescendant(withText(withText)))))
    }

    fun assertActivityFinishing(activity: MainActivityTestRule) = assertTrue(activity.activity.isFinishing)

    fun waitUntilYouTubeHomeLoads() {
        device.wait(Until.findObject(By.res(R.id.navUrlInput.toString()).textContains("youtube.com/tv#/surface?c=FEtopics&resume")), 5000)
    }

    fun waitUntilYouTubeVideoLoads() {
        device.wait(Until.findObject(By.res(R.id.navUrlInput.toString()).textContains("youtube.com/tv#/watch")), 5000)
    }

    // Disable the SessionLoadingIdlingResource temporarily. This is needed for the test for #1830
    // which requires opening the overlay before loading has completed.
    fun disableSessionIdling(activityTestRule: MainActivityTestRule) { activityTestRule.loadingIdlingResource.ignoreLoading = true }
    fun enableSessionIdling(activityTestRule: MainActivityTestRule) { activityTestRule.loadingIdlingResource.ignoreLoading = false }

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
            device.wait(Until.findObject(By.res(R.id.pocketVideoMegaTileView.toString())), 1000)
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

        /*
         * Navigate to the settings button using keypresses and open, and maintain focus.
         * Using click() to select buttons removes focus, so this is an alternative way to open
         * Settings.
         */
        fun linearNavigateToSettingsAndOpen(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            // We hard-code this navigiation pattern because making a generic way to linearly navigate
            // is very difficult within Espresso. Espresso supports asserting view state, but not
            // querying it. Because of this, we can't write conditional logic based on the currently
            // focused view.
            device.apply {
                // This will need to change if the button layout changes. However, such layout
                // changes are infrequent, and updating this will be easy.
                pressDPadUp()
                repeat(5) { pressDPadRight() }
                pressDPadCenter()
            }

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
private fun overlay() = onView(withId(R.layout.fragment_navigation_overlay))
private fun desktopModeButton() = onView(withId(R.id.desktopModeButton))
private fun pocketMegaTile() = onView(withId(R.id.pocketVideosContainer))
