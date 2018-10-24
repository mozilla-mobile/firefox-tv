/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData

object LiveData {

    // If more advanced versions of this are required, we should import RxJava instead of duplicating their effort
    fun <T, U, R> combineLatest(
        source1: LiveData<T>,
        source2: LiveData<U>,
        combiner: (T, U) -> R
    ): LiveData<R> {
        val result = MediatorLiveData<R>()

        val merge = merge@{
            val source1Value = source1.value ?: return@merge
            val source2Value = source2.value ?: return@merge

            result.value = combiner(source1Value, source2Value)
        }

        result.addSource(source1) { merge.invoke() }
        result.addSource(source2) { merge.invoke() }

        return result
    }
}
