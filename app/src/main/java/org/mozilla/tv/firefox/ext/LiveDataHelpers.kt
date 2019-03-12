/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

fun <T, U> LiveData<T>.map(mapper: (T) -> U): LiveData<U> {
    return Transformations.map(this, mapper)
}

fun <T> LiveData<T>.doOnEach(action: (T?) -> Unit): LiveData<T> {
    return this.map {
        action(it)
        it
    }
}

fun <T> MutableLiveData<T>.postIfNew(new: T) {
    val old = this.value
    if (new != old) this.postValue(new)
}

object LiveDataCombiners {

    // If more advanced versions of this are required, we should import RxJava instead of duplicating their effort #1783
    // Note: CursorViewModel.isEnabled combines 3 streams by calling this twice
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
