/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

/**
 * Generates a basic heuristic for whether or not a user is having difficulty
 * understanding the menu overlay.
 *
 * If a user opens a menu and then closes it without performing any action
 * (represented here by clicking select), it is assumed that they were unable
 * to find what they were looking for. This is used as a heuristic for
 * confusion.
 */
object MenuInteractionMonitor {
    private var selectPressedDuringCurrentMenu = true

    fun menuOpened() {
        selectPressedDuringCurrentMenu = false
    }

    fun menuClosed() {
        if (!selectPressedDuringCurrentMenu) TelemetryWrapper.menuUnusedEvent()
    }

    fun selectPressed() {
        selectPressedDuringCurrentMenu = true
    }
}
