/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

import org.mozilla.tv.firefox.pocket.PocketRepoState.Loading
import org.mozilla.tv.firefox.pocket.PocketRepoState.FetchFailed
import org.mozilla.tv.firefox.pocket.PocketRepoState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketRepoState.NoKey

class PocketRepoStateMachineTest {

    private val loadComplete = LoadComplete(listOf())
    private val goodResponse = listOf(PocketFeedItem.Video(0, "", "", "", 0))
    private val pocketRepoStateMachine = PocketRepoStateMachine()

    @Test
    fun `WHEN fromFetch is called with valid videos THEN ouput state should be load complete`() {
        val fromLoading = pocketRepoStateMachine.fromFetch(goodResponse, Loading)
        assertTrue(fromLoading is LoadComplete)

        val fromFailure = pocketRepoStateMachine.fromFetch(goodResponse, FetchFailed)
        assertTrue(fromFailure is LoadComplete)

        val fromComplete = pocketRepoStateMachine.fromFetch(goodResponse, loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.fromFetch(goodResponse, NoKey)
        assertTrue(fromNoKey is LoadComplete)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is failure THEN output state should be loading`() {
        val outputState = pocketRepoStateMachine.setLoading(FetchFailed)
        assertEquals(Loading, outputState)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is not failure THEN output state should equal input state`() {
        val fromLoading = pocketRepoStateMachine.setLoading(Loading)
        assertEquals(Loading, fromLoading)

        val fromComplete = pocketRepoStateMachine.setLoading(loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.setLoading(NoKey)
        assertEquals(NoKey, fromNoKey)
    }

    @Test
    fun `GIVEN null response WHEN cached state is loading THEN output state should be failure`() {
        val fromNull = pocketRepoStateMachine.fromFetch(null, Loading)
        assertEquals(FetchFailed, fromNull)
    }

    @Test
    fun `GIVEN null response WHEN cached state is not loading THEN output state should equal input state`() {
        val fromFailure = pocketRepoStateMachine.fromFetch(null, FetchFailed)
        assertEquals(FetchFailed, fromFailure)

        val fromComplete = pocketRepoStateMachine.fromFetch(null, loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.fromFetch(null, NoKey)
        assertEquals(NoKey, fromNoKey)
    }

    @Test
    fun `GIVEN empty response WHEN cached state is loading THEN output state should be failure`() {
        val fromNull = pocketRepoStateMachine.fromFetch(emptyList(), Loading)
        assertEquals(FetchFailed, fromNull)
    }

    @Test
    fun `GIVEN empty response WHEN cached state is not loading THEN output state should equal input state`() {
        val fromFailure = pocketRepoStateMachine.fromFetch(emptyList(), FetchFailed)
        assertEquals(FetchFailed, fromFailure)

        val fromComplete = pocketRepoStateMachine.fromFetch(emptyList(), loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.fromFetch(emptyList(), NoKey)
        assertEquals(NoKey, fromNoKey)
    }
}
