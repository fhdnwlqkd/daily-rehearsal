package com.rehearsal.domain.rehearsal.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import java.util.Optional;

@Description("turn evaluation 비동기 job 상태를 저장/조회하는 port")
public interface TurnEvaluationJobStore {

  void save(TurnEvaluationJob job);

  Optional<TurnEvaluationJob> findById(String sessionId, int turnNo);
}
