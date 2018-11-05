/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.Loading
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.FetchFailed
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.LoadComplete
import org.mozilla.tv.firefox.pocket.PocketVideoRepo.FeedState.NoAPIKey

class PocketFeedStateMachineTest {

    private val loadComplete = LoadComplete(listOf())
    private val goodResponse = listOf(PocketViewModel.FeedItem.Video(0, "", "", "", 0, JSONObject(mapOf("" to mapOf("" to "")))))
    private val pocketRepoStateMachine = PocketFeedStateMachine()

    @Test
    fun `WHEN cache state is null THEN output state should be input state`() {
        val fromLoading = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), null)
        assertTrue(fromLoading is LoadComplete)

        val fromFailure = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), null)
        assertTrue(fromFailure is LoadComplete)

        val fromComplete = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), null)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), null)
        assertTrue(fromNoKey is LoadComplete)
    }

    @Test
    fun `WHEN input state is load complete THEN ouput state should be load complete`() {
        val fromLoading = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), Loading)
        assertTrue(fromLoading is LoadComplete)

        val fromFailure = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), FetchFailed)
        assertTrue(fromFailure is LoadComplete)

        val fromComplete = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.computeNewState(LoadComplete(goodResponse), NoAPIKey)
        assertTrue(fromNoKey is LoadComplete)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is failure THEN output state should be loading`() {
        val outputState = pocketRepoStateMachine.computeNewState(Loading, FetchFailed)
        assertEquals(Loading, outputState)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is not failure THEN output state should equal cached state`() {
        val fromLoading = pocketRepoStateMachine.computeNewState(Loading, Loading)
        assertEquals(Loading, fromLoading)

        val fromComplete = pocketRepoStateMachine.computeNewState(Loading, loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.computeNewState(Loading, NoAPIKey)
        assertEquals(NoAPIKey, fromNoKey)
    }

    @Test
    fun `GIVEN input state is fetch failed WHEN cached state is loading THEN output state should be fetch failed`() {
        val fromFailed = pocketRepoStateMachine.computeNewState(FetchFailed, Loading)
        assertEquals(FetchFailed, fromFailed)
    }

    @Test
    fun `GIVEN input state is fetch failed WHEN cached state is not loading THEN output state should equal cached state`() {
        val fromFailure = pocketRepoStateMachine.computeNewState(FetchFailed, FetchFailed)
        assertEquals(FetchFailed, fromFailure)

        val fromComplete = pocketRepoStateMachine.computeNewState(FetchFailed, loadComplete)
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = pocketRepoStateMachine.computeNewState(FetchFailed, NoAPIKey)
        assertEquals(NoAPIKey, fromNoKey)
    }
}
