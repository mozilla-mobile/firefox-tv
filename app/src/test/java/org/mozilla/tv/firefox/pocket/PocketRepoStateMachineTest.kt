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

    @Test
    fun `WHEN input state is load complete THEN ouput state should be load complete`() {
        val fromLoading = PocketRepoStateMachine(loadComplete, Loading).computedState()
        assertTrue(fromLoading is LoadComplete)

        val fromFailure = PocketRepoStateMachine(loadComplete, FetchFailed).computedState()
        assertTrue(fromFailure is LoadComplete)

        val fromComplete = PocketRepoStateMachine(loadComplete, loadComplete).computedState()
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = PocketRepoStateMachine(loadComplete, NoKey).computedState()
        assertTrue(fromNoKey is LoadComplete)
    }

    @Test
    fun `WHEN input state is no key THEN output state should be no key`() {
        val fromLoading = PocketRepoStateMachine(NoKey, Loading).computedState()
        assertEquals(NoKey, fromLoading)

        val fromFailure = PocketRepoStateMachine(NoKey, FetchFailed).computedState()
        assertEquals(NoKey, fromFailure)

        val fromComplete = PocketRepoStateMachine(NoKey, loadComplete).computedState()
        assertEquals(NoKey, fromComplete)

        val fromNoKey = PocketRepoStateMachine(NoKey, NoKey).computedState()
        assertEquals(NoKey, fromNoKey)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is failure THEN output state should be loading`() {
        val outputState = PocketRepoStateMachine(Loading, FetchFailed).computedState()
        assertEquals(Loading, outputState)
    }

    @Test
    fun `GIVEN input state is loading WHEN cached state is not failure THEN output state should equal input state`() {
        val fromLoading = PocketRepoStateMachine(Loading, Loading).computedState()
        assertEquals(Loading, fromLoading)

        val fromComplete = PocketRepoStateMachine(Loading, loadComplete).computedState()
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = PocketRepoStateMachine(Loading, NoKey).computedState()
        assertEquals(NoKey, fromNoKey)
    }

    @Test
    fun `GIVEN input state is failure WHEN cached state is loading THEN output state should be failure`() {
        val outputState = PocketRepoStateMachine(FetchFailed, Loading).computedState()
        assertEquals(FetchFailed, outputState)
    }

    @Test
    fun `GIVEN input state is failure WHEN cached state is not loading THEN output state should equal input state`() {
        val fromFailure = PocketRepoStateMachine(FetchFailed, FetchFailed).computedState()
        assertEquals(FetchFailed, fromFailure)

        val fromComplete = PocketRepoStateMachine(FetchFailed, loadComplete).computedState()
        assertTrue(fromComplete is LoadComplete)

        val fromNoKey = PocketRepoStateMachine(FetchFailed, NoKey).computedState()
        assertEquals(NoKey, fromNoKey)
    }
}
