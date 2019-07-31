/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.tests

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.mozilla.tv.firefox.helpers.RxTestHelper

class ForceRxSynchronousTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            RxTestHelper.forceRxSynchronousInBeforeClass()
        }
    }

    @Test
    fun `GIVEN forceRxSynchronous WHEN various rx schedulers are used THEN all should operate on the same thread`() {
        val nameSet = listOf(
            Schedulers.computation(),
            Schedulers.io(),
            Schedulers.trampoline(),
            Schedulers.newThread(),
            Schedulers.single()
        )
            .map { Observable.just(1).observeOn(it).toThreadName() }
            .toSet()

        assertEquals(1, nameSet.size)
    }
}

private fun <T> Observable<T>.toThreadName(): String = this
    .map { Thread.currentThread().name }
    .blockingFirst()
