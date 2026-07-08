package com.rehearsal.api.support;

import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationJobStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTurnEvaluationJobStore implements TurnEvaluationJobStore {

  private final Map<String, TurnEvaluationJob> store = new HashMap<>();

  public InMemoryTurnEvaluationJobStore(TurnEvaluationJob... jobs) {
    for (TurnEvaluationJob job : jobs) {
      save(job);
    }
  }

  @Override
  public void save(TurnEvaluationJob job) {
    store.put(key(job.sessionId(), job.turnNo()), job);
  }

  @Override
  public Optional<TurnEvaluationJob> findById(String sessionId, int turnNo) {
    return Optional.ofNullable(store.get(key(sessionId, turnNo)));
  }

  private String key(String sessionId, int turnNo) {
    return sessionId + ":" + turnNo;
  }
}
