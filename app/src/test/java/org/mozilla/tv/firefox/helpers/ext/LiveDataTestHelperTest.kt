/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.ext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Rule
import org.junit.Test

class LiveDataTestHelperTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValues WHEN actual list is too short THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValuesWithReceiver WHEN actual list is too short THEN test fails`() {
        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertThat WHEN actual list is too short THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertThat({ it == 1 }, { it == 2 }, { it == 3 }, { it == 4 }) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValues WHEN actual list is too long THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
            ld.value = 5
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValuesWithReceiver WHEN actual list is too long THEN test fails`() {
        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
            value = 4
            value = 5
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertThat WHEN actual list is too long THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertThat({ it == 1 }, { it == 2 }, { it == 3 }, { it == 4 }) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
            ld.value = 5
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValues WHEN actual list is incorrect THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 3
            ld.value = 3
            ld.value = 4
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValuesWithReceiver WHEN actual list is incorrect THEN test fails`() {
        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 4
            value = 2
            value = 3
            value = 4
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertThat WHEN actual list is incorrect THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertThat({ it == 1 }, { it == 2 }, { it == 3 }, { it == 4 }) {
            ld.value = 1
            ld.value = 3
            ld.value = 3
            ld.value = 4
        }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValues WHEN actual list is empty THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) { }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertValuesWithReceiver WHEN actual list is empty THEN test fails`() {
        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) { }
    }

    @Test(expected = AssertionError::class)
    fun `GIVEN assertThat WHEN actual list is empty THEN test fails`() {
        val ld = MutableLiveData<Int>()
        ld.assertThat({ it == 1 }, { it == 2 }, { it == 3 }, { it == 4 }) {}
    }

    @Test
    fun `GIVEN assertValues WHEN actual list is correct THEN test passes`() {
        val ld = MutableLiveData<Int>()
        ld.assertValues(1, 2, 3, 4) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
        }
    }

    @Test
    fun `GIVEN assertValuesWithReceiver WHEN actual list is correct THEN test passes`() {
        MutableLiveData<Int>().assertValuesWithReceiver(1, 2, 3, 4) {
            value = 1
            value = 2
            value = 3
            value = 4
        }
    }

    @Test
    fun `GIVEN assertThat WHEN actual list is correct THEN test passes`() {
        val ld = MutableLiveData<Int>()
        ld.assertThat({ it == 1 }, { it == 2 }, { it == 3 }, { it == 4 }) {
            ld.value = 1
            ld.value = 2
            ld.value = 3
            ld.value = 4
        }
    }
}
