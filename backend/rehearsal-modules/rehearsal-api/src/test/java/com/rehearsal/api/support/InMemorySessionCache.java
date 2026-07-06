package com.rehearsal.api.support;

import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemorySessionCache implements SessionCache {

  private final Map<String, ClientSession> store = new HashMap<>();

  public InMemorySessionCache(ClientSession... sessions) {
    for (ClientSession session : sessions) {
      save(session);
    }
  }

  @Override
  public ClientSession save(ClientSession session) {
    store.put(session.getSessionId(), session);
    return session;
  }

  @Override
  public Optional<ClientSession> findById(String sessionId) {
    return Optional.ofNullable(store.get(sessionId));
  }
}
