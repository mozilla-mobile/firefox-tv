/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui.robots

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.pinnedtile.TileViewHolder

class PocketRecommendedVideosRobot {

    class Transition {

        fun openTileToBrowser(index: Int, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            rowContent().perform(RecyclerViewActions.actionOnItemAtPosition<TileViewHolder>(index, click()))

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun rowContent() = onView(withId(R.id.row_content))
