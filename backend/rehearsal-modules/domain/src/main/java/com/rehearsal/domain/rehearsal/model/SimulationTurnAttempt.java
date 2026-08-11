package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import lombok.Getter;

@Getter
@Description("User answer attempt and asynchronous evaluation state")
public class SimulationTurnAttempt {

  public static final int MAX_ATTEMPT = 2;

  private final Long id;
  private final Long simulationTurnId;
  private final int attemptNo;
  private final String userTranscript;
  private EvaluationStatus evaluationStatus;
  private TurnEvaluationOutcome outcome;
  private String feedback;
  private Boolean fallback;
  private String failureReason;

  private SimulationTurnAttempt(
      Long id,
      Long simulationTurnId,
      int attemptNo,
      String userTranscript,
      EvaluationStatus evaluationStatus,
      TurnEvaluationOutcome outcome,
      String feedback,
      Boolean fallback,
      String failureReason) {
    this.id = id;
    this.simulationTurnId = simulationTurnId;
    this.attemptNo = attemptNo;
    this.userTranscript = userTranscript;
    this.evaluationStatus = evaluationStatus;
    this.outcome = outcome;
    this.feedback = feedback;
    this.fallback = fallback;
    this.failureReason = failureReason;
  }

  public static SimulationTurnAttempt pending(
      Long simulationTurnId, int attemptNo, String userTranscript) {
    if (attemptNo < 1 || attemptNo > MAX_ATTEMPT) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
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
      TurnEvaluationOutcome outcome,
      String feedback,
      Boolean fallback,
      String failureReason) {
    return new SimulationTurnAttempt(
        id,
        simulationTurnId,
        attemptNo,
        userTranscript,
        evaluationStatus,
        outcome,
        feedback,
        fallback,
        failureReason);
  }

  public void complete(TurnEvaluationResult result) {
    validatePending();
    this.evaluationStatus = EvaluationStatus.COMPLETED;
    this.outcome = result.outcome();
    this.feedback = result.feedback();
    this.fallback = result.fallback();
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    validatePending();
    this.evaluationStatus = EvaluationStatus.FAILED;
    this.outcome = null;
    this.feedback = null;
    this.fallback = null;
    this.failureReason = failureReason;
  }

  public boolean canRetry() {
    return attemptNo < MAX_ATTEMPT;
  }

  private void validatePending() {
    if (evaluationStatus != EvaluationStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
