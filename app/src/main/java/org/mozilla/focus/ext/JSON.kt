/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Map over the JSONArray, removing null values after [transform] and ignoring invalid data.
 * An empty List will be returned if all the values are null or invalid.
 */
inline fun <R : Any> JSONArray.flatMapObj(transform: (JSONObject) -> R?): List<R> {
    val transformedResults = mutableListOf<R>()
    for (i in 0 until this.length()) {
        try {
            val transformed = transform(this.getJSONObject(i))
            if (transformed != null) { transformedResults.add(transformed) }
        } catch (e: JSONException) { /* Do nothing: we skip bad data. */ }
    }

    return transformedResults
}
