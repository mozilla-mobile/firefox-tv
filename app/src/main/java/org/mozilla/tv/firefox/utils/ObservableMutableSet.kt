/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

/**
 * A [MutableSet] that, on mutation, calls the observer functions provided to [attachObserver].
 *
 * This implementation is not thread safe.
 *
 * In general, using Rx Observables will produce more reliable code. However, when it's necessary to
 * reduce allocations, this Set implementation is a good alternative.
 *
 * We should consider replacing this with an a-c solution or a third-party implementation (a-c #2845).
 */
class ObservableMutableSet<T> private constructor(
    private val wrappedSet: MutableSet<T>
) : MutableSet<T> by wrappedSet {

    private val observers = mutableListOf<(ObservableMutableSet<T>) -> Unit>()

    /**
     * Attaches an observer function: this will be called when the set is mutated.
     */
    fun attachObserver(observer: (ObservableMutableSet<T>) -> Unit) {
        observers.add(observer)
    }

    /**
     * Removes a previously attached observer function.
     */
    fun detachObserver(observer: (ObservableMutableSet<T>) -> Unit) {
        observers.remove(observer)
    }

    override fun add(element: T): Boolean = wrappedSet.add(element).alsoNotifyObservers()
    override fun addAll(elements: Collection<T>): Boolean = wrappedSet.addAll(elements).alsoNotifyObservers()

    override fun remove(element: T): Boolean = wrappedSet.remove(element).alsoNotifyObservers()
    override fun removeAll(elements: Collection<T>): Boolean = wrappedSet.removeAll(elements).alsoNotifyObservers()
    override fun retainAll(elements: Collection<T>): Boolean = wrappedSet.retainAll(elements).alsoNotifyObservers()

    override fun clear() = wrappedSet.clear().alsoNotifyObservers()

    private fun <T> T.alsoNotifyObservers(): T {
        observers.forEach { it.invoke(this@ObservableMutableSet) }
        return this@alsoNotifyObservers
    }

    companion object {
        fun <T> from(setToWrap: MutableSet<T>): ObservableMutableSet<T> = ObservableMutableSet(setToWrap)
    }
}

fun <T> MutableSet<T>.toObservableMutableSet(): ObservableMutableSet<T> = ObservableMutableSet.from(this)
