package com.rehearsal.api.rehearsal.controller.dto;

import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;

public record SimulationStartResponse(
    String sessionId,
    int currentTurn,
    int maxTurn,
    TurnGenerationMode generationMode,
    String sceneCue,
    String opponentLine,
    String actionPrompt) {

  public static SimulationStartResponse from(SimulationStart result) {
    return new SimulationStartResponse(
        result.sessionId(),
        result.currentTurn(),
        result.maxTurn(),
        result.generationMode(),
        result.plan().sceneCue(),
        result.plan().opponentLine(),
        result.plan().actionPrompt());
  }
}
