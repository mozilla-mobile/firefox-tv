/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.tests

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.BeforeClass
import org.junit.Test
import org.mozilla.tv.firefox.helpers.RxTestHelper
import java.util.concurrent.TimeUnit

class ForceRxTestSchedulerTest {

    companion object {
        private lateinit var testScheduler: TestScheduler

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            testScheduler = RxTestHelper.forceRxTestSchedulerInBeforeClass()
        }
    }

    @Test
    fun `GIVEN forceRxTestScheduler WHEN various rx schedulers are used THEN all should operate on the same test scheduler`() {
        val initialValue = 1

        val comp = Observable.just(initialValue)
            .observeOn(Schedulers.computation())

        val io = Observable.just(initialValue)
            .observeOn(Schedulers.io())

        val trampoline = Observable.just(initialValue)
            .observeOn(Schedulers.trampoline())

        val newThread = Observable.just(initialValue)
            .observeOn(Schedulers.newThread())

        val single = Observable.just(initialValue)
            .observeOn(Schedulers.single())

        val testObservables = listOf(comp, io, trampoline, newThread, single)
            .map { it.delay(10, TimeUnit.HOURS) }
            .map { it.test() }

        testObservables.forEach {
            it.assertNoValues()
        }

        testScheduler.advanceTimeTo(11, TimeUnit.HOURS)

        testObservables.forEach {
            it.assertValue(initialValue)
        }
    }
}
