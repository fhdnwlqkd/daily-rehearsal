package com.rehearsal.api.session.application;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Temporary in-memory session store for MVP API wiring. This will be replaced by Redis-backed
 * storage in a later issue.
 */
@Component
public class InMemorySessionStore {

  private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();

  public SessionState save(SessionState sessionState) {
    sessions.put(sessionState.getSessionId(), sessionState);
    return sessionState;
  }

  public Optional<SessionState> findById(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }
}
