/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.ext

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.junit.Rule
import org.junit.Test

class LiveDataTestHelperTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test(expected = AssertionError::class)
    fun `WHEN actual list is too short THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
        }

        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
        }
    }

    @Test(expected = AssertionError::class)
    fun `WHEN actual list is too long THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
            ld.value = 5
        }

        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
            value = 4
            value = 5
        }
    }

    @Test(expected = AssertionError::class)
    fun `WHEN actual list is incorrect THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 3
            ld.value = 3
            ld.value = 4
        }

        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 4
            value = 2
            value = 3
            value = 4
        }
    }

    @Test(expected = AssertionError::class)
    fun `WHEN actual list is empty THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) { }

        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) { }
    }

    @Test
    fun `WHEN actual list is correct THEN test passes`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
        }

        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
            value = 4
        }
    }
}
