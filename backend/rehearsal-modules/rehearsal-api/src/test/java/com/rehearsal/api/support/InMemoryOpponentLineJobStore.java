package com.rehearsal.api.support;

import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.port.OpponentLineJobStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryOpponentLineJobStore implements OpponentLineJobStore {

  private final Map<String, OpponentLineJob> store = new HashMap<>();

  public InMemoryOpponentLineJobStore(OpponentLineJob... jobs) {
    for (OpponentLineJob job : jobs) {
      save(job);
    }
  }

  @Override
  public void save(OpponentLineJob job) {
    store.put(key(job.sessionId(), job.turnNo()), job);
  }

  @Override
  public Optional<OpponentLineJob> findById(String sessionId, int turnNo) {
    return Optional.ofNullable(store.get(key(sessionId, turnNo)));
  }

  private String key(String sessionId, int turnNo) {
    return sessionId + ":" + turnNo;
  }
}
