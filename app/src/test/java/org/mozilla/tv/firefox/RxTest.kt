/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import io.reactivex.subjects.PublishSubject
import org.junit.Test

/**
 * This class is for testing combinations of operators to ensure that they work
 * as we expect.
 *
 * Rx is hard.  Don't wing it; test.
 */
class RxTest {

    @Test
    fun `GIVEN a source is set to cache its most recent value AND an observer has subscribed andthen disposed WHEN a new observer subscribes THEN only the most recent value should be emitted`() {
        // This is an important pattern for usage in UI. State will often be
        // observed by a fragment, which is then destroyed and replaced by a
        // new fragment. We do not want state to be recomputed when this
        // happens, instead the most recent value should be retained.
        //
        // This is the same caching behavior as LiveData.
        val subject = PublishSubject.create<Int>()
        val source = subject
            .replay(1)
            .autoConnect(0)

        subject.onNext(0)
        subject.onNext(1)

        val observable1 = source.test()

        observable1.assertValues(1)

        subject.onNext(2)

        observable1.assertValues(1, 2)

        subject.onNext(3)

        val observable2 = source.test()

        observable1.assertValues(1, 2, 3)
        observable2.assertValues(3)

        observable1.dispose()
        observable2.dispose()

        val observable3 = source.test()

        observable3.assertValues(3)
    }
}
