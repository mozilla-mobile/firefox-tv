/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.hint

import androidx.annotation.DrawableRes

/**
 * Backing data for hint to be displayed on the bottom hint bar
 */
data class Hint(val text: String, @DrawableRes val icon: Int)
