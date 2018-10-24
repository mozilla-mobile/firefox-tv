/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.ext

import android.arch.lifecycle.MutableLiveData
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.helpers.SkipOnboardingMainActivityTestRule

@RunWith(AndroidJUnit4::class)
@Suppress("TestFunctionName")
// Espresso test functions can't have spaces in their names, even between backticks. This prevents
// the linting error for starting a function with an uppercase letter
class LiveDataMapTest {

    @Rule @JvmField
    val activityTestRule = SkipOnboardingMainActivityTestRule()
    private lateinit var source: MutableLiveData<Int>

    @Before
    fun setup() {
        source = MutableLiveData()
    }

    @Test
    fun WHEN_map_is_called_on_source_THEN_source_is_not_modified() {
        activityTestRule.runOnUiThread {
            source.observeForever { assertEquals(1, it) }
            source.map { it.toString() }
            source.map { it * 5 }
            source.value = 1
        }
    }

    @Test
    fun WHEN_simple_maps_THEN_emit_expected_values() {
        activityTestRule.runOnUiThread {
            val mapped1 = source.map { it.toString() }
            val mapped2 = source.map { it * 5 }
            mapped1.observeForever { assertEquals("1", it) }
            mapped2.observeForever { assertEquals(5, it) }
            source.value = 1
        }
    }

    @Test
    fun WHEN_maps_are_chained_THEN_emit_expected_values() {
        activityTestRule.runOnUiThread {
            val mapped = source
                .map { it * 5 }
                .map { it.toString() }
            mapped.observeForever { assertEquals("5", it) }
            source.value = 1
        }
    }
}
