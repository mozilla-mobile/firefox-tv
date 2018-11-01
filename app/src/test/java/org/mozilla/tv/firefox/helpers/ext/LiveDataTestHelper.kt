/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.ext

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import org.junit.Assert.fail
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

fun <T> LiveData<T>.assertValues(vararg expectedRaw: T, pushValues: () -> Unit) {
    // Arrays do not print prettily, so convert them to a list
    val expectedValues = List(expectedRaw.size) { expectedRaw[it] }

    val actualValues = mutableListOf<T>()

    val observer = spy(Observer<T> {
        it ?: return@Observer
        actualValues += it
    })

    this.observeForever(observer)

    pushValues()

    if (actualValues.size > expectedValues.size) fail("LiveData emitted more values than expected\nExpected: $expectedValues\nActual  : $actualValues")
    if (actualValues.size < expectedValues.size) fail("LiveData emitted fewer values than expected\nExpected: $expectedValues\nActual  : $actualValues")

    expectedValues.zip(actualValues).forEachIndexed { i, (expect, actual) ->
        if (expect != actual) fail("Values emitted at index $i do not match\nExpected: $expectedValues\nActual  : $actualValues")
    }

    verify(observer, times(expectedValues.size)).onChanged(any())
}

fun <T> MutableLiveData<T>.assertValuesWithReceiver(vararg expectedRaw: T, pushValues: MutableLiveData<T>.() -> Unit) {
    this.assertValues(*expectedRaw) { this.pushValues() }
}
