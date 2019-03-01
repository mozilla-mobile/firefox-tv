/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.helpers.ext.assertValuesWithReceiver
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule

@Suppress("TestFunctionName")
class LiveDataPostIfNewTest {

    @get:Rule val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var source: MutableLiveData<Int?>
    private lateinit var observerSpy: Observer<Int?>

    @Before
    fun setup() {
        source = MutableLiveData()
        observerSpy = spy(Observer { /* only used to verify call count */ })
        source.observeForever(observerSpy)
    }

    @Test
    fun `GIVEN old value was posted WHEN the same value is posted again THEN only one should be emitted`() {
        source.assertValuesWithReceiver(1) {
            postIfNew(1)
            postIfNew(1)
        }
    }

    @Test
    fun `GIVEN old value is posted WHEN a different value is posted THEN both should be emitted`() {
        source.assertValuesWithReceiver(1, 2) {
            postIfNew(1)
            postIfNew(2)
        }
    }

    @Test
    fun `GIVEN old value was posted AND a different value was posted WHEN the original value is posted again THEN three total values should have been emitted`() {
        source.assertValuesWithReceiver(1, 2, 1) {
            postIfNew(1)
            postIfNew(2)
            postIfNew(1)
        }
    }

    @Test
    fun `GIVEN previous state was null WHEN new state is pushed THEN new state should be emitted`() {
        source.value = null
        source.postIfNew(1)
        observerSpy = spy(Observer { assertEquals(1, it) })

        source.observeForever(observerSpy)
        verify(observerSpy, times(1)).onChanged(any())
    }
}
