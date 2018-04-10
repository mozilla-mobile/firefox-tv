/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.architecture.NonNullLiveData;
import org.mozilla.focus.architecture.NonNullMutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sessions are managed by this global SessionManager instance.
 */
public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private NonNullMutableLiveData<List<Session>> sessions;
    private String currentSessionUUID;

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    private SessionManager() {
        this.sessions = new NonNullMutableLiveData<>(
                Collections.unmodifiableList(Collections.<Session>emptyList()));
    }

    /**
     * Is there at least one browsing session?
     */
    public boolean hasSession() {
        return !sessions.getValue().isEmpty();
    }

    /**
     * Get the current session. This method will throw an exception if there's no active session.
     */
    public Session getCurrentSession() {
        if (currentSessionUUID == null) {
            throw new IllegalAccessError("There's no active session");
        }

        return getSessionByUUID(currentSessionUUID);
    }

    public boolean isCurrentSession(@NonNull Session session) {
        return session.getUUID().equals(currentSessionUUID);
    }

    public boolean hasSessionWithUUID(@NonNull String uuid) {
        for (Session session : sessions.getValue()) {
            if (uuid.equals(session.getUUID())) {
                return true;
            }
        }

        return false;
    }

    public Session getSessionByUUID(@NonNull String uuid) {
        for (Session session : sessions.getValue()) {
            if (uuid.equals(session.getUUID())) {
                return session;
            }
        }

        throw new IllegalAccessError("There's no active session with UUID " + uuid);
    }

    public int getNumberOfSessions() {
        return sessions.getValue().size();
    }

    public int getPositionOfCurrentSession() {
        if (currentSessionUUID == null) {
            return -1;
        }

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session session = this.sessions.getValue().get(i);

            if (session.getUUID().equals(currentSessionUUID)) {
                return i;
            }
        }

        return -1;
    }

    public NonNullLiveData<List<Session>> getSessions() {
        return sessions;
    }

    public void createSession(@NonNull Source source, @NonNull String url) {
        final Session session = new Session(source, url);
        addSession(session);
    }

    private void addSession(Session session) {
        currentSessionUUID = session.getUUID();

        final List<Session> sessions = new ArrayList<>(this.sessions.getValue());
        sessions.add(session);

        this.sessions.setValue(Collections.unmodifiableList(sessions));
    }

    /**
     * Remove all sessions.
     */
    public void removeAllSessions() {
        currentSessionUUID = null;

        sessions.setValue(Collections.unmodifiableList(Collections.<Session>emptyList()));
    }

    /**
     * Remove the current (selected) session.
     */
    public void removeCurrentSession() {
        removeSession(currentSessionUUID);
    }

    @VisibleForTesting void removeSession(String uuid) {
        final List<Session> sessions = new ArrayList<>();

        int removedFromPosition = -1;

        for (int i = 0; i < this.sessions.getValue().size(); i++) {
            final Session currentSession = this.sessions.getValue().get(i);

            if (currentSession.getUUID().equals(uuid)) {
                removedFromPosition = i;
                continue;
            }

            sessions.add(currentSession);
        }

        if (removedFromPosition == -1) {
            return;
        }

        if (sessions.isEmpty()) {
            currentSessionUUID = null;
        } else {
            final Session currentSession = sessions.get(
                    Math.min(removedFromPosition, sessions.size() - 1));
            currentSessionUUID = currentSession.getUUID();
        }

        this.sessions.setValue(sessions);
    }
}
