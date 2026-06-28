package com.rehearsal.domain.session.cache;

import com.rehearsal.domain.session.model.ClientSession;
import java.util.Optional;

public interface SessionCache {

  ClientSession save(ClientSession session);

  Optional<ClientSession> findById(String sessionId);
}
