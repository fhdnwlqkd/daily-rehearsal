package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import lombok.Getter;

@Getter
@Description("Simulation turn and opponent-line generation state")
public class SimulationTurn {

  private final Long id;
  private final String sessionId;
  private final int turnNo;
  private TurnGenerationMode generationMode;
  private OpponentLineStatus opponentLineStatus;
  private SimulationTurnPlan plan;
  private String failureReason;

  private SimulationTurn(
      Long id,
      String sessionId,
      int turnNo,
      TurnGenerationMode generationMode,
      OpponentLineStatus opponentLineStatus,
      SimulationTurnPlan plan,
      String failureReason) {
    this.id = id;
    this.sessionId = sessionId;
    this.turnNo = turnNo;
    this.generationMode = generationMode;
    this.opponentLineStatus = opponentLineStatus;
    this.plan = plan;
    this.failureReason = failureReason;
  }

  public static SimulationTurn pending(
      String sessionId, int turnNo, TurnGenerationMode generationMode) {
    return new SimulationTurn(
        null, sessionId, turnNo, generationMode, OpponentLineStatus.PENDING, null, null);
  }

  public static SimulationTurn completed(
      String sessionId, int turnNo, TurnGenerationMode generationMode, SimulationTurnPlan plan) {
    return new SimulationTurn(
        null, sessionId, turnNo, generationMode, OpponentLineStatus.COMPLETED, plan, null);
  }

  public static SimulationTurn restore(
      Long id,
      String sessionId,
      int turnNo,
      TurnGenerationMode generationMode,
      OpponentLineStatus opponentLineStatus,
      SimulationTurnPlan plan,
      String failureReason) {
    return new SimulationTurn(
        id, sessionId, turnNo, generationMode, opponentLineStatus, plan, failureReason);
  }

  public void complete(SimulationTurnPlan plan) {
    validatePending();
    this.opponentLineStatus = OpponentLineStatus.COMPLETED;
    this.plan = plan;
    this.failureReason = null;
  }

  public void completeWithTechnicalFallback(SimulationTurnPlan plan) {
    validatePending();
    this.generationMode = TurnGenerationMode.TECHNICAL_FALLBACK;
    this.opponentLineStatus = OpponentLineStatus.COMPLETED;
    this.plan = plan;
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    validatePending();
    this.opponentLineStatus = OpponentLineStatus.FAILED;
    this.plan = null;
    this.failureReason = failureReason;
  }

  private void validatePending() {
    if (opponentLineStatus != OpponentLineStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
