
// TODO add this stuff

package org.mozilla.tv.firefox

import android.support.annotation.VisibleForTesting

/**
 * TODO
 */
class ScreenControllerStateMachine(private val currentUrlIsHome: () -> Boolean) {

    enum class ActiveScreen {
        NAVIGATION_OVERLAY, WEB_RENDER, POCKET, SETTINGS
    }

    enum class Transition {
        PUSH_OVERLAY, PUSH_POCKET, PUSH_SETTINGS, EXIT_APP, NO_OP, POP_BACK_STACK
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var currentState: ActiveScreen = ActiveScreen.NAVIGATION_OVERLAY

    fun menuPress(): Transition {
        return Transition.PUSH_OVERLAY
    }

    fun backPress(): Transition {
        return Transition.PUSH_OVERLAY
    }

    fun overlayClosed() {

    }

    fun pocketOpened() {

    }

    fun settingsOpened() {

    }
}