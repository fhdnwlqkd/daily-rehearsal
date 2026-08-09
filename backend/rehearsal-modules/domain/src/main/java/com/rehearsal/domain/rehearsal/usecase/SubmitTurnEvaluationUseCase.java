package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;

public interface SubmitTurnEvaluationUseCase {

  SimulationTurnAttempt submit(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics);
}
