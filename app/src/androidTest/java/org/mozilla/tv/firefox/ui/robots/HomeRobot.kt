/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.net.Uri
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import android.support.test.espresso.matcher.ViewMatchers.hasDescendant
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.not
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.pinnedtile.TileViewHolder
import org.mozilla.tv.firefox.helpers.ext.assertDisabled
import org.mozilla.tv.firefox.helpers.ext.assertEnabled
import org.mozilla.tv.firefox.helpers.ext.click

/**
 * Implementation of Robot Pattern for the home menu.
 */
class HomeRobot {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun openMenu() {
        overlay().check(matches(not(withEffectiveVisibility(VISIBLE))))
        device.pressMenu()
    }

    fun closeMenu() {
        overlay().check(matches(withEffectiveVisibility(VISIBLE)))
        device.pressMenu()
    }

    fun goBack() = backButton().click()
    fun goForward() = forwardButton().click()
    fun reload() = reloadButton().click()
    fun toggleTurbo() = turboButton().click()
    fun openSettings() = settingsButton().click()

    fun assertCanGoBack() = backButton().assertEnabled()
    fun assertCannotGoBack() = backButton().assertDisabled()
    fun assertCanGoForward() = forwardButton().assertEnabled()
    fun assertCannotGoForward() = forwardButton().assertDisabled()

    fun navigateToPage(url: Uri) {
        urlBar().perform(replaceText(url.toString()), pressImeActionButton())
    }

    fun openTile(index: Int) {
        homeTiles().perform(RecyclerViewActions.actionOnItemAtPosition<TileViewHolder>(index, click()))
    }

    fun openTile(title: String) {
        homeTiles().perform(RecyclerViewActions.actionOnItem<TileViewHolder>(hasDescendant(withText(title)), click()))
    }
}

/**
 * Applies [func] to a new [HomeRobot]
 *
 * @sample org.mozilla.tv.firefox.session.ClearSessionTest.WHEN_data_is_cleared_THEN_back_and_forward_should_be_unavailable
 */
fun home(func: HomeRobot.() -> Unit) = HomeRobot().apply(func)

private fun backButton() = onView(withId(R.id.navButtonBack))
private fun forwardButton() = onView(withId(R.id.navButtonForward))
private fun reloadButton() = onView(withId(R.id.navButtonReload))
private fun pinButton() = onView(withId(R.id.pinButton))
private fun turboButton() = onView(withId(R.id.turboButton))
private fun settingsButton() = onView(withId(R.id.navButtonSettings))
private fun urlBar() = onView(withId(R.id.navUrlInput))
private fun homeTiles() = onView(withId(R.id.tileContainer))
private fun overlay() = onView(withId(R.id.browserOverlay))
