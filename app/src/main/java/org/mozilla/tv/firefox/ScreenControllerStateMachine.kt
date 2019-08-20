/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

/**
 * State machine for which Fragment is currently visible in the app.
 */
object ScreenControllerStateMachine {

    enum class ActiveScreen {
        NAVIGATION_OVERLAY, WEB_RENDER, SETTINGS, FXA_PROFILE
    }

    enum class Transition {
        ADD_OVERLAY, REMOVE_OVERLAY, ADD_SETTINGS_DATA,
        ADD_SETTINGS_COOKIES, REMOVE_SETTINGS, SHOW_BROWSER, EXIT_APP, NO_OP,
        ADD_FXA_PROFILE, REMOVE_FXA_PROFILE
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
            ActiveScreen.SETTINGS -> Transition.REMOVE_SETTINGS
            ActiveScreen.FXA_PROFILE -> Transition.REMOVE_FXA_PROFILE
        }
    }

    fun getNewStateBackPress(currentActiveScreen: ActiveScreen, canGoBack: Boolean): Transition {
        return when (currentActiveScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                return if (canGoBack) {
                    Transition.REMOVE_OVERLAY
                } else {
                    Transition.EXIT_APP
                }
            }
            ActiveScreen.WEB_RENDER -> { // The browser handles webview back presses first
                Transition.ADD_OVERLAY
            }
            ActiveScreen.SETTINGS -> {
                Transition.REMOVE_SETTINGS
            }
            ActiveScreen.FXA_PROFILE -> Transition.REMOVE_FXA_PROFILE
        }
    }
}
