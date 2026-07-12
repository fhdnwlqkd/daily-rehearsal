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
  private OpponentLineStatus opponentLineStatus;
  private String opponentLine;
  private String failureReason;

  private SimulationTurn(
      Long id,
      String sessionId,
      int turnNo,
      OpponentLineStatus opponentLineStatus,
      String opponentLine,
      String failureReason) {
    this.id = id;
    this.sessionId = sessionId;
    this.turnNo = turnNo;
    this.opponentLineStatus = opponentLineStatus;
    this.opponentLine = opponentLine;
    this.failureReason = failureReason;
  }

  public static SimulationTurn pending(String sessionId, int turnNo) {
    return new SimulationTurn(
        null, sessionId, turnNo, OpponentLineStatus.PENDING, null, null);
  }

  public static SimulationTurn completed(String sessionId, int turnNo, String opponentLine) {
    return new SimulationTurn(
        null, sessionId, turnNo, OpponentLineStatus.COMPLETED, opponentLine, null);
  }

  public static SimulationTurn restore(
      Long id,
      String sessionId,
      int turnNo,
      OpponentLineStatus opponentLineStatus,
      String opponentLine,
      String failureReason) {
    return new SimulationTurn(
        id, sessionId, turnNo, opponentLineStatus, opponentLine, failureReason);
  }

  public void complete(String opponentLine) {
    validatePending();
    this.opponentLineStatus = OpponentLineStatus.COMPLETED;
    this.opponentLine = opponentLine;
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    validatePending();
    this.opponentLineStatus = OpponentLineStatus.FAILED;
    this.opponentLine = null;
    this.failureReason = failureReason;
  }

  private void validatePending() {
    if (opponentLineStatus != OpponentLineStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
    }
  }
}
