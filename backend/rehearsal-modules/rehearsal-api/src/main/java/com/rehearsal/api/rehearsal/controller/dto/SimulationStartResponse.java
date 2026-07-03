package com.rehearsal.api.rehearsal.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.SimulationStart;

@Description("시뮬레이션 시작 시 현재 turn과 첫 상대 발화를 담는 응답")
public record SimulationStartResponse(
    String sessionId, int currentTurn, int maxTurn, String opponentLine) {

  public static SimulationStartResponse from(SimulationStart result) {
    return new SimulationStartResponse(
        result.sessionId(), result.currentTurn(), result.maxTurn(), result.opponentLine());
  }
}
