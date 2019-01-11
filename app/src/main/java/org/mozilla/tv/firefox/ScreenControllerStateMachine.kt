/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

/**
 * State machine for which Fragment is currently visible in the app.
 */
object ScreenControllerStateMachine {

    enum class ActiveScreen {
        NAVIGATION_OVERLAY, WEB_RENDER, POCKET, SETTINGS
    }

    enum class Transition {
        ADD_OVERLAY, REMOVE_OVERLAY, ADD_POCKET, REMOVE_POCKET, ADD_SETTINGS, REMOVE_SETTINGS, SHOW_BROWSER,
        EXIT_APP, NO_OP
    }

    fun getNewStateMenuPress(currentActiveScreen: ActiveScreen, isUrlHome: Boolean): Transition {
        return when (currentActiveScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                return if (isUrlHome) {
                    Transition.NO_OP
                } else {
                    Transition.REMOVE_OVERLAY
                }
            }
            ActiveScreen.WEB_RENDER -> {
                Transition.ADD_OVERLAY
            }
            ActiveScreen.POCKET -> Transition.REMOVE_POCKET
            ActiveScreen.SETTINGS -> Transition.REMOVE_SETTINGS
        }
    }

    fun getNewStateBackPress(currentActiveScreen: ActiveScreen, isUrlHome: Boolean): Transition {
        return when (currentActiveScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                return if (isUrlHome) {
                    Transition.EXIT_APP
                } else {
                    Transition.REMOVE_OVERLAY
                }
            }
            ActiveScreen.WEB_RENDER -> { // The browser handles webview back presses first
                Transition.ADD_OVERLAY
            }
            ActiveScreen.POCKET -> {
                Transition.REMOVE_POCKET
            }
            ActiveScreen.SETTINGS -> {
                Transition.REMOVE_SETTINGS
            }
        }
    }
}
