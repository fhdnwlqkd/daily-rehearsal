package com.rehearsal.api.support;

import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import java.util.Optional;

public class InMemorySessionCache extends InMemorySessionRepository implements SessionCache {

  public InMemorySessionCache(ClientSession... sessions) {
    super(sessions);
  }

  @Override
  public ClientSession save(ClientSession session) {
    return saveSession(session);
  }

  @Override
  public Optional<ClientSession> findById(String sessionId) {
    return findSession(sessionId);
  }
}
