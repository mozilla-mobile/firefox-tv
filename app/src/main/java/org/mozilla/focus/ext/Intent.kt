/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Intent
import org.mozilla.focus.utils.SafeIntent

fun Intent.toSafeIntent() = SafeIntent(this)
