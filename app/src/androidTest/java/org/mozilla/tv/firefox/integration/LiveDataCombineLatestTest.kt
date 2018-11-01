/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.integration

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.ext.LiveData

@Suppress("TestFunctionName")
// Espresso test functions can't have spaces in their names, even between backticks. This prevents
// the linting error for starting a function with an uppercase letter
class LiveDataCombineLatestTest {

    @Rule @JvmField val activityTestRule = MainActivityTestRule()

    private lateinit var source1: MutableLiveData<Int>
    private lateinit var source2: MutableLiveData<String>
    private lateinit var result: MutableLiveData<Pair<Int, String>>

    @Before
    fun setup() {
        source1 = MutableLiveData()
        source2 = MutableLiveData()

        result = LiveData.combineLatest(source1, source2) { v1, v2 -> Pair(v1, v2) } as MutableLiveData<Pair<Int, String>>
    }

    @Test
    fun WHEN_the_first_source_never_emits_THEN_the_result_should_never_emit() {
        activityTestRule.runOnUiThread {
            result.observeForever { fail("Shouldn't emit anything") }

            source2.value = "A"
            source2.value = "B"
            source2.value = "C"
        }
    }

    @Test
    fun WHEN_the_second_source_never_emits_THEN_the_result_should_never_emit() {
        activityTestRule.runOnUiThread {
            result.observeForever { fail("Shouldn't emit anything") }

            source1.value = 1
            source1.value = 2
            source1.value = 3
        }
    }

    @Test
    fun WHEN_both_sources_emit_THEN_the_result_should_emit_pairs() {
        activityTestRule.runOnUiThread {
            var observer = Observer<Pair<Int, String>> { assertEquals(Pair(1, "A"), it) }

            result.observeForever(observer)

            source1.value = 1
            source2.value = "A"

            result.removeObserver(observer)

            source1.value = 2
            source2.value = "B"

            observer = Observer { assertEquals(Pair(2, "B"), it) }
            result.observeForever(observer)
        }
    }

    @Test
    fun GIVEN_a_value_has_already_been_emitted_WHEN_only_one_source_emits_THEN_the_result_should_use_cached_value() {
        activityTestRule.runOnUiThread {
            source1.value = 1
            source2.value = "A"

            source1.value = 2

            result.observeForever { assertEquals(Pair(2, "A"), it) }
        }
    }
}
