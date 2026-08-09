package com.rehearsal.api.rehearsal.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpponentLineResponse(
    String sessionId,
    int turnNo,
    OpponentLineStatus status,
    String opponentLine,
    String failureReason) {

  public static OpponentLineResponse from(SimulationTurn turn) {
    return new OpponentLineResponse(
        turn.getSessionId(),
        turn.getTurnNo(),
        turn.getOpponentLineStatus(),
        turn.getPlan() == null ? null : turn.getPlan().opponentLine(),
        turn.getFailureReason());
  }
}
