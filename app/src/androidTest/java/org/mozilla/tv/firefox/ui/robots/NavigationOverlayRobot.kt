/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.net.Uri
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import mozilla.components.support.android.test.espresso.assertIsChecked
import mozilla.components.support.android.test.espresso.assertIsDisplayed
import mozilla.components.support.android.test.espresso.assertIsEnabled
import mozilla.components.support.android.test.espresso.assertIsSelected
import mozilla.components.support.android.test.espresso.click
import mozilla.components.support.android.test.espresso.matcher.hasFocus
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertTrue
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.NestedScrollToAction
import org.mozilla.tv.firefox.helpers.RecyclerViewHelpers
import org.mozilla.tv.firefox.channels.DefaultChannelTileViewHolder

/**
 * Implementation of Robot Pattern for the navigation overlay menu.
 */
class NavigationOverlayRobot {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun goBack() = backButton().click()
    fun goForward() = forwardButton().click()
    fun reload() = reloadButton().click()
    fun toggleTurbo() = turboButton().click()

    fun remoteBack() = device.pressBack()
    fun remoteUp() = device.pressDPadUp()
    fun remoteRight(x: Int = 1) = repeat(x) { device.pressDPadRight() }
    fun remoteCenter() = device.pressDPadCenter()
    /*
     * Navigate to the settings channel using keypresses
     */
    fun linearNavigateToSettings() {

        var settingsTileSelected = false

        device.apply {
            while (!settingsTileSelected) {
                try {
                    onView(allOf(withId(R.id.settings_cardview), hasFocus(true))).assertIsDisplayed(true)
                    settingsTileSelected = true
                } catch (ex: NoMatchingViewException) {
                    pressDPadDown()
                }
            }
        }
    }

    // The implementation of this method is arbitrary. We could run this check
    // against any of its views
    fun assertOverlayIsOpen() = urlBar().assertIsDisplayed(true)

    fun assertCanGoBack(canGoBack: Boolean) {
        device.wait(Until.findObject(By.res(R.id.channelsContainer.toString())), 5000)
        backButton().assertIsEnabled(canGoBack)
    }
    fun assertCanGoForward(canGoForward: Boolean) = forwardButton().assertIsEnabled(canGoForward)
    fun assertCanGoBackForward(canGoBack: Boolean, canGoForward: Boolean) {
        device.wait(Until.findObject(By.res(R.id.navUrlInput.toString())), 5000)
        backButton().assertIsEnabled(canGoBack)
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

    fun assertTooltipText(text: String) {
        // This wait is to ensure the check does not happen when UI paint is not completed
        device.wait(Until.findObject(By.res(R.id.tooltip.toString())), 5000)

        // The tooltip popup locator differs by the Android OS Version, and we need to accommodate for both 4K stick and Gen 2
        // Gen 2 Stick = 22
        // 4K Stick = 25 (N_MR1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            tooltip().check(matches(withText(text)))
        } else {
            tooltip_nonPlatformPopup().check(matches(withText(text)))
        }
    }

    class Transition {
        private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun enterUrlAndEnterToBrowser(url: Uri, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            urlBar().perform(clearText(),
                    typeText(url.toString()),
                    closeSoftKeyboard(),
                    pressImeActionButton())
            waitUntilSiteLoaded()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun closeToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            device.pressMenu()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openTileToBrowser(index: Int, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            homeTiles().perform(
                    NestedScrollToAction(),
                    RecyclerViewActions.actionOnItemAtPosition<DefaultChannelTileViewHolder>(index, click())
            )
            waitUntilSiteLoaded()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun linearNavigateToTelemtryTileAndOpen(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            var settingsTileSelected = false

            device.apply {
                while (!settingsTileSelected) {
                    try {
                        onView(allOf(withId(R.id.settings_cardview), hasFocus(true))).assertIsDisplayed(true)
                        settingsTileSelected = true
                        pressDPadCenter()
                    } catch (ex: NoMatchingViewException) {
                        pressDPadDown()
                    }
                }
            }

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openSettingsCleardataTile(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            settingsCleardataTile().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openSettingsAboutTile(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            settingsAboutTile().click()

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
            waitUntilSiteLoaded()
            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goForward(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            forwardButton().click()
            waitUntilSiteLoaded()
            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun reload(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            reloadButton().click()
            waitUntilSiteLoaded()
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

        private fun waitUntilSiteLoaded() {
            device.wait(Until.findObject(By.res(R.id.progressAnimation.toString())), 2000)
            device.wait(Until.gone(By.res(R.id.progressAnimation.toString())), 5000)
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
private fun urlBar() = onView(withId(R.id.navUrlInput))
private fun homeTiles() = onView(withId(R.id.pinned_tiles_channel))
private fun overlay() = onView(withId(R.layout.fragment_navigation_overlay))
private fun desktopModeButton() = onView(withId(R.id.desktopModeButton))
private fun tooltip() = onView(withId(R.id.tooltip)).inRoot(isPlatformPopup())
private fun tooltip_nonPlatformPopup() = onView(withId(R.id.tooltip)).inRoot(withDecorView(withId(R.id.tooltip)))
private fun settingsTelemetryTile() = onView(withId(R.id.settings_tile_telemetry))
private fun settingsCleardataTile() = onView(withId(R.id.settings_tile_cleardata))
private fun settingsAboutTile() = onView(withId(R.id.settings_tile_about))
private fun settingsPrivacypolicyTile() = onView(withId(R.id.settings_tile_privacypolicy))
