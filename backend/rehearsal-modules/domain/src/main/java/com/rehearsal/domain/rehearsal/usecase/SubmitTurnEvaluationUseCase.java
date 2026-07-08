package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;

public interface SubmitTurnEvaluationUseCase {

  TurnEvaluationJob submit(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics);
}
