package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("evaluation API 응답과 turn 결과 저장에 함께 사용하는 turn 판정 결과")
public record TurnEvaluationResult(
    TurnEvaluationOutcome outcome, String feedback, boolean fallback) {

  public static TurnEvaluationResult classify(
      boolean accepted, int attemptNo, int maxAttempt, String feedback, boolean fallback) {
    if (attemptNo < 1 || attemptNo > maxAttempt) {
      throw new IllegalArgumentException("attemptNo is outside the configured range");
    }
    TurnEvaluationOutcome outcome =
        accepted
            ? TurnEvaluationOutcome.ACCEPTED
            : attemptNo < maxAttempt
                ? TurnEvaluationOutcome.RETRY_REQUIRED
                : TurnEvaluationOutcome.FORCED_ADVANCE;
    return new TurnEvaluationResult(outcome, feedback, fallback);
  }
}
