/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.support.v7.widget.RecyclerView
import android.support.test.espresso.matcher.BoundedMatcher
import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher

// Taken from https://stackoverflow.com/a/34795431
fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
    checkNotNull(itemMatcher)
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position)
                ?: return false // has no item on such position
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}
