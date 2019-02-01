/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.components

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.tv.firefox.utils.anyNonNull

class ApplicationLifecycleSessionCacheTest {

    private lateinit var cache: ApplicationLifecycleSessionCache
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionManager = mock(SessionManager::class.java)
        cache = ApplicationLifecycleSessionCache(sessionManager)
    }

    @Test
    fun `WHEN on destroy cache is called THEN all the sessions are removed from the session manager`() {
        cache.onDestroyCacheSessions()
        verify(sessionManager, times(1)).removeAll()
    }

    @Test
    fun `WHEN on destroy cache is called THEN the cache contains the sessions in order`() {
        val expected = listOf(mock(Session::class.java), mock(Session::class.java))
        `when`(sessionManager.all).thenReturn(expected)

        cache.onDestroyCacheSessions()

        cache.cachedSessions.forEachIndexed { i, session ->
            assertSame(expected[i], session)
        }
    }

    @Test
    fun `GIVEN there are no sessions in the cache WHEN onCreate is called THEN there are no sessions in the SessionManager`() {
        cache.onCreateMaybeRestoreSessions()
        verifySessionManagerAdd(expectedTimes = 0)
    }

    @Test
    fun `GIVEN there are sessions in the cache WHEN on create is called THEN the sessions are restored`() {
        val expected = listOf(mock(Session::class.java), mock(Session::class.java)).also { sessions ->
            sessions.forEach { cache.cachedSessions.add(it) }
        }

        cache.onCreateMaybeRestoreSessions()

        verifySessionManagerAdd(expectedTimes = expected.size)
    }

    private fun verifySessionManagerAdd(expectedTimes: Int) {
        verify(sessionManager, times(expectedTimes)).add(
            // Mockito doesn't known about default values. :(
            anyNonNull(),
            anyBoolean(),
            any(),
            any()
        )
    }
}
