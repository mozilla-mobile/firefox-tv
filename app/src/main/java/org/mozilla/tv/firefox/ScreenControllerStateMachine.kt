/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.support.annotation.VisibleForTesting

/**
 * State machine for which Fragment is currently visible in the app.
 */
class ScreenControllerStateMachine(private val currentUrlIsHome: () -> Boolean) {

    enum class ActiveScreen {
        NAVIGATION_OVERLAY, WEB_RENDER, POCKET, SETTINGS
    }

    enum class Transition {
        ADD_OVERLAY, REMOVE_OVERLAY, ADD_POCKET, REMOVE_POCKET, ADD_SETTINGS, REMOVE_SETTINGS, EXIT_APP, NO_OP
    }

    var currentActiveScreen: ActiveScreen = ActiveScreen.POCKET
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun menuPress(): Transition {
        return when (currentActiveScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                return if (currentUrlIsHome()) {
                    Transition.NO_OP
                } else {
                    currentActiveScreen = ActiveScreen.WEB_RENDER
                    Transition.REMOVE_OVERLAY
                }
            }
            ActiveScreen.WEB_RENDER -> {
                currentActiveScreen = ActiveScreen.NAVIGATION_OVERLAY
                Transition.ADD_OVERLAY
            }
            ActiveScreen.POCKET -> Transition.NO_OP
            ActiveScreen.SETTINGS -> Transition.NO_OP
        }
    }

    fun backPress(): Transition {
        return when (currentActiveScreen) {
            ActiveScreen.NAVIGATION_OVERLAY -> {
                return if (currentUrlIsHome()) {
                    Transition.EXIT_APP
                } else {
                    currentActiveScreen = ActiveScreen.WEB_RENDER
                    Transition.REMOVE_OVERLAY
                }
            }
            ActiveScreen.WEB_RENDER -> {
                currentActiveScreen = ActiveScreen.NAVIGATION_OVERLAY
                Transition.ADD_OVERLAY
            }
            ActiveScreen.POCKET -> {
                currentActiveScreen = ActiveScreen.NAVIGATION_OVERLAY
                Transition.REMOVE_POCKET
            }
            ActiveScreen.SETTINGS -> {
                currentActiveScreen = ActiveScreen.NAVIGATION_OVERLAY
                Transition.REMOVE_SETTINGS
            }
        }
    }

    fun overlayClosed() {
        currentActiveScreen = ActiveScreen.WEB_RENDER
    }

    fun overlayOpened() {
        currentActiveScreen = ActiveScreen.NAVIGATION_OVERLAY
    }

    fun pocketOpened() {
        currentActiveScreen = ActiveScreen.POCKET
    }

    fun webRenderLoaded() {
        currentActiveScreen = ActiveScreen.WEB_RENDER
    }

    fun settingsOpened() {
        currentActiveScreen = ActiveScreen.SETTINGS
    }
}
