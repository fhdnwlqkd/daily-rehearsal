package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;

public interface GetTurnEvaluationUseCase {

  SimulationTurnAttempt get(String sessionId, int turnNo);
}
