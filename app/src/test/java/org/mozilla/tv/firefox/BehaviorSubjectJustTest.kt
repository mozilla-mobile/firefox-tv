package org.mozilla.tv.firefox

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BehaviorSubjectJustTest {

    fun getValue(): String {
        count++
        return "Value"
    }

    var count = 0

    @Before
    fun setup() {
        count = 0
    }

    @Test // Thjs fails
    // Subject extends Observable, and so inherits methods like just and fromCallable, but does not
    // override or change them in any way. BehaviorSubject.fromCallable(x) is exactly equivelent to
    // Observable.fromCallable(x), which means that followup subscriptions will reexecute the initial
    // code block
    fun `GIVEN behaviorsubject is created from callable WHEN it is called twice THEN init code should not reexecute`() {
        val subject = BehaviorSubject.fromCallable { getValue() }

        subject.subscribe()
        subject.subscribe()

        assertEquals(1, count) // THIS IS UNTRUE
    }

    @Test
    // Replay(1) caches the most recently emitted value, and autoConnect(0) kicks off execution as
    // soon as 0 observers have subscribed (i.e., immediately). This means that the code block is
    // not reexecuted, and followup subscribers just receive the cached value
    fun `GIVEN observable is created using replay and autoconnect WHEN it is called twice THEN init code should not reexecute`() {
        val observable = Observable.fromCallable { getValue()  }
            .replay(1)
            .autoConnect(0)

        observable.subscribe()
        observable.subscribe()

        assertEquals(1, count)
    }

    @Test
    // This passes, but not because of the type. Instead, it works because of the nature of `just`.
    // `just` doesn't accept a callable, it accepts a normal value. So getValue is executed once
    // before BehaviorSubject.just, and is resolved to "Value". The observable then knows to emit
    // that String any time an Observer subscribes, but it never actually reexecutes the call to
    // getValue
    fun `GIVEN behaviorsubject is created using just WHEN it is called twice THEN init code should not reexecute`() {
        val subject = BehaviorSubject.just(getValue()) // This is not actually a BehaviorSubject. It doesn't cache anything

        subject.subscribe()
        subject.subscribe()

        assertEquals(1, count) // This is true
    }

    @Test
    // In any instance where we're using `just` to create our Observable, we should be fine not
    // using a BehaviorSubject, not adding `replay` and `autoConnect` calls. Using `just` by itself
    // should be enough, because it's going to compute once when the Observable is created, and
    // never again.
    fun `GIVEN observable is created using just WHEN it is called twice THEN init code should not reexecute`() {
        val observable = Observable.just(getValue())

        observable.subscribe()
        observable.subscribe()

        assertEquals(1, count)
    }
}
