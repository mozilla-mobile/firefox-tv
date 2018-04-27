/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import com.squareup.picasso.Picasso

/**
 * A holder for the shared Picasso instance. All calls to Picasso should go through this class.
 */
object PicassoWrapper {
    @JvmStatic
    val client get() = Picasso.get()
}
