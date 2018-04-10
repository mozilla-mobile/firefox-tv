/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SessionManagerTest {
    private static final String TEST_URL = "https://github.com/mozilla-mobile/focus-android";
    private static final String TEST_URL_2 = "https://www.mozilla.org";
    private static final String TEST_URL_3 = "https://www.mozilla.org/en-US/firefox/focus/";

    @Before
    public void setUp() {
        // Always start tests with a clean session manager
        SessionManager.getInstance().removeAllSessions();
    }

    @Test
    public void testInitialState() {
        final SessionManager sessionManager = SessionManager.getInstance();

        assertNotNull(sessionManager.getSessions().getValue());
        assertEquals(0, sessionManager.getSessions().getValue().size());
        assertFalse(sessionManager.hasSession());
    }

    @Test(expected = IllegalAccessError.class)
    public void getCurrentSessionThrowsExceptionIfThereIsNoSession() {
        final SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.getCurrentSession();
    }

    @Test
    public void testHasSessionWithUUID() {
        final SessionManager sessionManager = SessionManager.getInstance();
        assertFalse(sessionManager.hasSessionWithUUID(UUID.randomUUID().toString()));

        sessionManager.createSession(Source.USER_ENTERED, TEST_URL);

        assertTrue(sessionManager.hasSession());

        final Session session = sessionManager.getCurrentSession();
        assertNotNull(session);
        assertTrue(sessionManager.hasSessionWithUUID(session.getUUID()));
        assertNotNull(sessionManager.getSessionByUUID(session.getUUID()));
    }

    @Test(expected = IllegalAccessError.class)
    public void getSessionByUUIDThrowsExceptionIfSessionDoesNotExist() {
        final SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.getSessionByUUID(UUID.randomUUID().toString());
    }

    @Test
    public void testRemovingSessions() {
        final SessionManager sessionManager = SessionManager.getInstance();

        sessionManager.createSession(Source.USER_ENTERED, TEST_URL);
        sessionManager.createSession(Source.VIEW, TEST_URL_2);

        {
            final List<Session> sessions = sessionManager.getSessions().getValue();
            assertEquals(2, sessions.size());
        }

        {
            final Session currentSession = sessionManager.getCurrentSession();
            assertEquals(Source.VIEW, currentSession.getSource());
            assertEquals(TEST_URL_2, currentSession.getUrl().getValue());
        }

        sessionManager.removeCurrentSession();

        {
            final Session currentSession = sessionManager.getCurrentSession();
            assertEquals(Source.USER_ENTERED, currentSession.getSource());
            assertEquals(TEST_URL, currentSession.getUrl().getValue());
        }

        sessionManager.removeCurrentSession();

        assertFalse(sessionManager.hasSession());
        assertEquals(0, sessionManager.getSessions().getValue().size());
    }

    @Test
    public void testRemovingAllSessions() {
        final SessionManager sessionManager = SessionManager.getInstance();

        sessionManager.createSession(Source.USER_ENTERED, TEST_URL);
        sessionManager.createSession(Source.VIEW, TEST_URL_2);

        assertTrue(sessionManager.hasSession());
        assertEquals(2, sessionManager.getSessions().getValue().size());

        sessionManager.removeAllSessions();

        assertFalse(sessionManager.hasSession());
        assertEquals(0, sessionManager.getSessions().getValue().size());
    }

    @Test
    public void testHasSessionWithUUIDWithUnknownUUID() {
        final SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.createSession(Source.USER_ENTERED, TEST_URL);

        assertFalse(sessionManager.hasSessionWithUUID(UUID.randomUUID().toString()));
    }

    @Test
    public void testRemovingUnknownSessionHasNoEffect() {
        final SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.removeSession(UUID.randomUUID().toString());

        assertFalse(sessionManager.hasSession());
        assertEquals(0, sessionManager.getSessions().getValue().size());
    }
}
