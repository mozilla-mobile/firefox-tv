/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.utils
import org.mockito.Mockito
/**
 * Use this when [Mockito.any] crashes.
 *
 * A normal [Mockito.any] is a nullable type, so this allows us to test nonnull
 * code.
 *
 * Taken from https://medium.com/@elye.project/befriending-kotlin-and-mockito-1c2e7b0ef791
 */
@Suppress("UNCHECKED_CAST")
fun <T> anyNonNull(): T {
    // Internally, this calls static void method reportMatcher, which seems to
    // set some class state.  If this line is commented out, the function will
    // not work
    Mockito.any<T>()
    return null as T
}