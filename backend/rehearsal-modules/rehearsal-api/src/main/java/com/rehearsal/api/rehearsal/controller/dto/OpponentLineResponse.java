package com.rehearsal.api.rehearsal.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpponentLineResponse(
    String sessionId,
    int turnNo,
    OpponentLineStatus status,
    TurnGenerationMode generationMode,
    String sceneCue,
    String opponentLine,
    String actionPrompt,
    String failureReason) {

  public static OpponentLineResponse from(SimulationTurn turn) {
    return new OpponentLineResponse(
        turn.getSessionId(),
        turn.getTurnNo(),
        turn.getOpponentLineStatus(),
        turn.getGenerationMode(),
        turn.getPlan() == null ? null : turn.getPlan().sceneCue(),
        turn.getPlan() == null ? null : turn.getPlan().opponentLine(),
        turn.getPlan() == null ? null : turn.getPlan().actionPrompt(),
        turn.getFailureReason());
  }
}
