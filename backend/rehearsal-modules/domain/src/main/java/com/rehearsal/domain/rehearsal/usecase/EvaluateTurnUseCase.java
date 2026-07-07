package com.rehearsal.domain.rehearsal.usecase;

import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;

public interface EvaluateTurnUseCase {

  TurnEvaluationResult evaluateTurn(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics);
}
