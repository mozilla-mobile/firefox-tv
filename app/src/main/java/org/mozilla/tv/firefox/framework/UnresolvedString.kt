/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.framework

import android.content.Context
import android.content.res.Resources

/**
 * Contains information required to resolve a string once a [Context] has been provided. This
 * allows us to determine strings in models without leaking [Context] into these layers.
 */
data class UnresolvedString(private val id: Int, private val formatArgs: List<String> = emptyList()) {
    // TODO handle unresolved strings where format args are also IDs

    fun resolve(context: Context): String = resolve(context.resources)

    fun resolve(resources: Resources): String = when (formatArgs.size) {
        0 -> resources.getString(id)
        1 -> resources.getString(id, formatArgs[0])
        2 -> resources.getString(id, formatArgs[0], formatArgs[1])
        else -> throw NotImplementedError("UnresolvedString#resolve does not currently support " +
            "that number of arguments. Please extend the class!")
    }
}
