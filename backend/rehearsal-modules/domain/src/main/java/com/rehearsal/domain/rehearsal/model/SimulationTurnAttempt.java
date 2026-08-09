package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import lombok.Getter;

@Getter
@Description("User answer attempt and asynchronous evaluation state")
public class SimulationTurnAttempt {

  private final Long id;
  private final Long simulationTurnId;
  private final int attemptNo;
  private final String userTranscript;
  private EvaluationStatus evaluationStatus;
  private Boolean success;
  private String feedback;
  private Boolean fallback;
  private String failureReason;

  private SimulationTurnAttempt(
      Long id,
      Long simulationTurnId,
      int attemptNo,
      String userTranscript,
      EvaluationStatus evaluationStatus,
      Boolean success,
      String feedback,
      Boolean fallback,
      String failureReason) {
    this.id = id;
    this.simulationTurnId = simulationTurnId;
    this.attemptNo = attemptNo;
    this.userTranscript = userTranscript;
    this.evaluationStatus = evaluationStatus;
    this.success = success;
    this.feedback = feedback;
    this.fallback = fallback;
    this.failureReason = failureReason;
  }

  public static SimulationTurnAttempt pending(
      Long simulationTurnId, int attemptNo, String userTranscript) {
    return new SimulationTurnAttempt(
        null,
        simulationTurnId,
        attemptNo,
        userTranscript,
        EvaluationStatus.PENDING,
        null,
        null,
        null,
        null);
  }

  public static SimulationTurnAttempt restore(
      Long id,
      Long simulationTurnId,
      int attemptNo,
      String userTranscript,
      EvaluationStatus evaluationStatus,
      Boolean success,
      String feedback,
      Boolean fallback,
      String failureReason) {
    return new SimulationTurnAttempt(
        id,
        simulationTurnId,
        attemptNo,
        userTranscript,
        evaluationStatus,
        success,
        feedback,
        fallback,
        failureReason);
  }

  public void complete(TurnEvaluationResult result) {
    validatePending();
    this.evaluationStatus = EvaluationStatus.COMPLETED;
    this.success = result.success();
    this.feedback = result.feedback();
    this.fallback = result.fallback();
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    validatePending();
    this.evaluationStatus = EvaluationStatus.FAILED;
    this.success = null;
    this.feedback = null;
    this.fallback = null;
    this.failureReason = failureReason;
  }

  private void validatePending() {
    if (evaluationStatus != EvaluationStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
