package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;

public interface GetTurnEvaluationUseCase {

  TurnEvaluationJob get(String sessionId, int turnNo);
}
