package com.rehearsal.api.rehearsal.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TurnEvaluationResponse(
    String sessionId,
    int turnNo,
    int attemptNo,
    EvaluationStatus status,
    Boolean success,
    String feedback,
    Boolean fallback,
    boolean turnCompleted,
    String failureReason) {

  public static TurnEvaluationResponse from(
      String sessionId, int turnNo, SimulationTurnAttempt attempt, boolean turnCompleted) {
    return new TurnEvaluationResponse(
        sessionId,
        turnNo,
        attempt.getAttemptNo(),
        attempt.getEvaluationStatus(),
        attempt.getSuccess(),
        attempt.getFeedback(),
        attempt.getFallback(),
        turnCompleted,
        attempt.getFailureReason());
  }
}
