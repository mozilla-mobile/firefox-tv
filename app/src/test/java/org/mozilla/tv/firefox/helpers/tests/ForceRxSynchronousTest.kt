/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.tests

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.mozilla.tv.firefox.helpers.forceRxSynchronous

class ForceRxSynchronousTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            forceRxSynchronous()
        }
    }

    @Test
    fun `GIVEN forceRxSynchronous WHEN various rx schedulers are used THEN all should operate on the same thread`() {
        val comp = Observable.just(1)
            .observeOn(Schedulers.computation())
            .toThreadName()

        val io = Observable.just(1)
            .observeOn(Schedulers.io())
            .toThreadName()

        val trampoline = Observable.just(1)
            .observeOn(Schedulers.trampoline())
            .toThreadName()

        val newThread = Observable.just(1)
            .observeOn(Schedulers.newThread())
            .toThreadName()

        val single = Observable.just(1)
            .observeOn(Schedulers.single())
            .toThreadName()

        val nameSet = setOf(comp, io, trampoline, newThread, single)

        println(nameSet)

        assertEquals(1, nameSet.size)
    }
}

private fun <T> Observable<T>.toThreadName(): String = this
    .map { Thread.currentThread().name }
    .blockingFirst()