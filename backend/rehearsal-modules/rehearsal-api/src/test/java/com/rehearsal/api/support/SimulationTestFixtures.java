package com.rehearsal.api.support;

import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;

public final class SimulationTestFixtures {

  private SimulationTestFixtures() {}

  public static SimulationTurn pendingTurn(String sessionId, int turnNo) {
    return SimulationTurn.pending(sessionId, turnNo, TurnGenerationMode.NORMAL);
  }

  public static SimulationTurn completedTurn(String sessionId, int turnNo, String opponentLine) {
    return SimulationTurn.completed(
        sessionId, turnNo, TurnGenerationMode.NORMAL, plan(opponentLine));
  }

  public static SimulationTurnPlan plan(String opponentLine) {
    return new SimulationTurnPlan(
        "상대가 대화를 이어갑니다.", opponentLine, "자연스럽게 답해보세요.", "상대의 말에 관련된 답을 한다.");
  }
}
